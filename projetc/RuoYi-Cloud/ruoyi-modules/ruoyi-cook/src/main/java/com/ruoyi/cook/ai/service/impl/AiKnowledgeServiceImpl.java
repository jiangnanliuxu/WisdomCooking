package com.ruoyi.cook.ai.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.cook.ai.config.RagProperties;
import com.ruoyi.cook.ai.domain.AiKnowledgeChunk;
import com.ruoyi.cook.ai.domain.AiKnowledgeDocument;
import com.ruoyi.cook.ai.domain.AiKnowledgeIndexJob;
import com.ruoyi.cook.ai.dto.AiKnowledgeTextRequest;
import com.ruoyi.cook.ai.mapper.AiKnowledgeChunkMapper;
import com.ruoyi.cook.ai.mapper.AiKnowledgeDocumentMapper;
import com.ruoyi.cook.ai.mapper.AiKnowledgeIndexJobMapper;
import com.ruoyi.cook.ai.service.IAiKnowledgeService;
import com.ruoyi.cook.ai.vo.AiKnowledgeDocumentVo;
import com.ruoyi.cook.ai.vo.AiKnowledgeIndexJobVo;
import com.ruoyi.cook.ai.vo.AiRagRetrieveResult;
import com.ruoyi.cook.ai.vo.AiRagSourceVo;
import com.ruoyi.cook.common.domain.vo.PageVo;
import jakarta.annotation.PreDestroy;

/**
 * 本地知识库管理与向量索引服务。
 */
@Service
public class AiKnowledgeServiceImpl implements IAiKnowledgeService
{
    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeServiceImpl.class);

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_INDEXED = "indexed";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_OFFLINE = "offline";
    private static final String JOB_RUNNING = "running";
    private static final String JOB_SUCCESS = "success";
    private static final String JOB_FAILED = "failed";
    private static final String JOB_PARTIAL_FAILED = "partial_failed";

    private final ExecutorService indexExecutor = Executors.newFixedThreadPool(2);

    @Autowired
    private RagProperties ragProperties;

    @Autowired
    private AiKnowledgeDocumentMapper documentMapper;

    @Autowired
    private AiKnowledgeChunkMapper chunkMapper;

    @Autowired
    private AiKnowledgeIndexJobMapper jobMapper;

    @Autowired
    private ObjectProvider<VectorStore> vectorStoreProvider;

    @PreDestroy
    public void shutdownIndexExecutor()
    {
        indexExecutor.shutdown();
    }

    @Override
    public PageVo<AiKnowledgeDocumentVo> listDocuments(String status, String keyword, Integer page, Integer pageSize)
    {
        PageHelper.startPage(normalizePage(page), normalizePageSize(pageSize));
        List<AiKnowledgeDocument> rows = documentMapper.selectList(status, keyword);
        PageInfo<AiKnowledgeDocument> info = new PageInfo<>(rows);
        return new PageVo<>(normalizePage(page), normalizePageSize(pageSize), info.getTotal(),
                rows.stream().map(this::toDocumentVo).toList());
    }

    @Override
    public AiKnowledgeDocumentVo createTextDocument(AiKnowledgeTextRequest request)
    {
        try
        {
            Path root = ensureDocsRoot();
            String fileName = normalizeInputFileName(request.getFileName());
            Path target = root.resolve(fileName).normalize();
            assertInsideRoot(root, target);
            Files.writeString(target, request.getContent(), StandardCharsets.UTF_8);
            return toDocumentVo(indexSingleFile(target, true));
        }
        catch (IOException e)
        {
            throw new ServiceException("保存知识库文本失败：" + e.getMessage());
        }
    }

    @Override
    public AiKnowledgeDocumentVo uploadDocument(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("上传文件不能为空");
        }
        try
        {
            Path root = ensureDocsRoot();
            String fileName = normalizeInputFileName(file.getOriginalFilename());
            Path target = avoidOverwrite(root.resolve(fileName).normalize());
            assertInsideRoot(root, target);
            file.transferTo(target.toFile());
            return toDocumentVo(indexSingleFile(target, true));
        }
        catch (IOException e)
        {
            throw new ServiceException("上传知识库文件失败：" + e.getMessage());
        }
    }

    @Override
    public AiKnowledgeIndexJobVo scanDocuments()
    {
        AiKnowledgeIndexJob job = createJob(null, "manual_scan");
        indexExecutor.execute(() -> runScanJob(job));
        return toJobVo(job);
    }

    @Override
    public AiKnowledgeIndexJobVo reindexDocument(Long id)
    {
        AiKnowledgeDocument document = requireDocument(id);
        AiKnowledgeIndexJob job = createJob(id, "reindex");
        indexExecutor.execute(() -> runReindexJob(job, document));
        return toJobVo(job);
    }

    @Override
    public AiKnowledgeDocumentVo offlineDocument(Long id)
    {
        AiKnowledgeDocument document = requireDocument(id);
        deleteVectors(document.getId());
        chunkMapper.deleteByDocumentId(document.getId());
        documentMapper.updateOffline(document.getId());
        return toDocumentVo(documentMapper.selectById(document.getId()));
    }

    @Override
    public PageVo<AiKnowledgeIndexJobVo> listJobs(Long documentId, Integer page, Integer pageSize)
    {
        PageHelper.startPage(normalizePage(page), normalizePageSize(pageSize));
        List<AiKnowledgeIndexJob> rows = jobMapper.selectList(documentId);
        PageInfo<AiKnowledgeIndexJob> info = new PageInfo<>(rows);
        return new PageVo<>(normalizePage(page), normalizePageSize(pageSize), info.getTotal(),
                rows.stream().map(this::toJobVo).toList());
    }

    @Override
    public void scanDocumentsOnStartup()
    {
        if (!ragProperties.isEnabled() || !ragProperties.isIngestOnStartup())
        {
            return;
        }
        indexExecutor.execute(() -> {
            try
            {
                runScanJob(createJob(null, "startup_scan"));
            }
            catch (Exception e)
            {
                log.warn("启动扫描知识库失败：{}", e.getMessage());
            }
        });
    }

    @Override
    public AiRagRetrieveResult retrieve(String question)
    {
        if (!ragProperties.isEnabled())
        {
            return AiRagRetrieveResult.miss("RAG未启用");
        }
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null)
        {
            return AiRagRetrieveResult.miss("向量库未初始化");
        }
        try
        {
            SearchRequest request = SearchRequest.builder()
                    .query(question)
                    .topK(Math.max(1, ragProperties.getTopK()))
                    .similarityThreshold(ragProperties.getSimilarityThreshold())
                    .build();
            List<Document> documents = vectorStore.similaritySearch(request);
            if (documents == null || documents.isEmpty())
            {
                return AiRagRetrieveResult.miss("知识库未命中");
            }
            List<AiRagSourceVo> sources = new ArrayList<>();
            StringBuilder context = new StringBuilder();
            int index = 1;
            for (Document document : documents)
            {
                AiRagSourceVo source = toRagSource(document);
                sources.add(source);
                context.append("[").append(index).append("] 知识库标题：")
                        .append(StringUtils.isBlank(source.getTitle()) ? resolveSourceName(source) : source.getTitle())
                        .append("\n")
                        .append(document.getText())
                        .append("\n\n");
                index++;
            }
            return new AiRagRetrieveResult(true, context.toString().trim(), sources, null);
        }
        catch (Exception e)
        {
            log.warn("知识库检索失败：{}", e.getMessage());
            return AiRagRetrieveResult.miss("知识库检索失败：" + e.getMessage());
        }
    }

    private void runScanJob(AiKnowledgeIndexJob job)
    {
        int indexed = 0;
        int failed = 0;
        try
        {
            Path root = ensureDocsRoot();
            List<Path> files = listSupportedFiles(root);
            job.setTotalDocuments(files.size());
            updateProgress(job, indexed, failed, "扫描到" + files.size() + "个知识库文件");
            for (Path file : files)
            {
                try
                {
                    AiKnowledgeDocument document = indexSingleFile(file, false);
                    if (STATUS_INDEXED.equals(document.getStatus()))
                    {
                        indexed++;
                    }
                }
                catch (Exception e)
                {
                    failed++;
                    log.warn("索引知识库文件失败：{} {}", file, e.getMessage());
                }
                updateProgress(job, indexed, failed, "已处理" + (indexed + failed) + "/" + files.size());
            }
            finishJob(job, failed == 0 ? JOB_SUCCESS : JOB_PARTIAL_FAILED,
                    "扫描完成，成功" + indexed + "个，失败" + failed + "个");
        }
        catch (Exception e)
        {
            finishJob(job, JOB_FAILED, "扫描失败：" + e.getMessage());
        }
    }

    private void runReindexJob(AiKnowledgeIndexJob job, AiKnowledgeDocument document)
    {
        try
        {
            job.setTotalDocuments(1);
            updateProgress(job, 0, 0, "开始重建文档索引");
            indexSingleFile(Paths.get(document.getFilePath()), true);
            updateProgress(job, 1, 0, "文档索引重建完成");
            finishJob(job, JOB_SUCCESS, "文档索引重建完成");
        }
        catch (Exception e)
        {
            updateProgress(job, 0, 1, "文档索引重建失败：" + e.getMessage());
            finishJob(job, JOB_FAILED, "文档索引重建失败：" + e.getMessage());
        }
    }

    private AiKnowledgeDocument indexSingleFile(Path file, boolean force)
    {
        if (!isSupportedFile(file))
        {
            throw new ServiceException("仅支持.md和.txt知识库文件");
        }
        try
        {
            Path root = ensureDocsRoot();
            Path normalized = file.toAbsolutePath().normalize();
            assertInsideRoot(root, normalized);
            String content = Files.readString(normalized, StandardCharsets.UTF_8);
            if (StringUtils.isBlank(content))
            {
                throw new ServiceException("知识库文件内容为空");
            }
            String relativePath = toRelativePath(root, normalized);
            String hash = sha256(normalized);
            long size = Files.size(normalized);
            AiKnowledgeDocument document = documentMapper.selectByRelativePath(relativePath);
            boolean unchanged = document != null
                    && Objects.equals(hash, document.getFileHash())
                    && Objects.equals(size, document.getFileSize())
                    && STATUS_INDEXED.equals(document.getStatus())
                    && chunkMapper.countByDocumentId(document.getId()) > 0
                    && !force;
            if (unchanged)
            {
                return document;
            }
            document = upsertDocumentSnapshot(document, normalized, relativePath, hash, size, content);
            indexDocumentContent(document, content);
            return documentMapper.selectById(document.getId());
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("索引知识库文件失败：" + e.getMessage());
        }
    }

    private AiKnowledgeDocument upsertDocumentSnapshot(AiKnowledgeDocument existed, Path file, String relativePath,
            String hash, long size, String content)
    {
        AiKnowledgeDocument document = existed == null ? new AiKnowledgeDocument() : existed;
        document.setFileName(file.getFileName().toString());
        document.setOriginalName(file.getFileName().toString());
        document.setRelativePath(relativePath);
        document.setFilePath(file.toString());
        document.setFileHash(hash);
        document.setFileSize(size);
        document.setFileType(extension(file));
        document.setTitle(extractTitle(content, file.getFileName().toString()));
        document.setStatus(STATUS_PENDING);
        document.setErrorMessage(null);
        if (existed == null)
        {
            document.setChunkCount(0);
            documentMapper.insertDocument(document);
        }
        else
        {
            documentMapper.updateFileSnapshot(document);
        }
        return document;
    }

    private void indexDocumentContent(AiKnowledgeDocument document, String content)
    {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null)
        {
            documentMapper.updateFailed(document.getId(), "向量库未初始化，请确认Elasticsearch和Spring AI VectorStore配置");
            throw new ServiceException("向量库未初始化");
        }
        try
        {
            documentMapper.updateIndexing(document.getId());
            deleteVectors(document.getId());
            chunkMapper.deleteByDocumentId(document.getId());

            List<String> parts = splitContent(content);
            List<Document> vectorDocuments = new ArrayList<>();
            int index = 0;
            for (String part : parts)
            {
                AiKnowledgeChunk chunk = new AiKnowledgeChunk();
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(index);
                chunk.setVectorId(buildVectorId(document, index));
                chunk.setContent(part);
                chunk.setTokenCount(estimateTokens(part));
                chunk.setStatus(STATUS_INDEXED);
                chunkMapper.insertChunk(chunk);
                vectorDocuments.add(Document.builder()
                        .id(chunk.getVectorId())
                        .text(part)
                        .metadata(Map.of(
                                "documentId", document.getId(),
                                "chunkId", chunk.getId(),
                                "fileName", document.getFileName(),
                                "relativePath", document.getRelativePath(),
                                "title", document.getTitle() == null ? "" : document.getTitle()))
                        .build());
                index++;
            }
            vectorStore.add(vectorDocuments);
            documentMapper.updateIndexed(document.getId(), vectorDocuments.size());
        }
        catch (Exception e)
        {
            chunkMapper.deleteByDocumentId(document.getId());
            documentMapper.updateFailed(document.getId(), e.getMessage());
            throw new ServiceException("向量化失败：" + e.getMessage());
        }
    }

    private List<String> splitContent(String content)
    {
        String text = content.replace("\r\n", "\n").trim();
        if (StringUtils.isBlank(text))
        {
            return Collections.emptyList();
        }
        int chunkSize = Math.max(200, ragProperties.getChunkSize());
        int overlap = Math.max(0, Math.min(ragProperties.getChunkOverlap(), chunkSize / 2));
        int step = Math.max(1, chunkSize - overlap);
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < text.length(); start += step)
        {
            int end = Math.min(text.length(), start + chunkSize);
            String chunk = text.substring(start, end).trim();
            if (StringUtils.isNotBlank(chunk))
            {
                chunks.add(chunk);
            }
            if (end == text.length())
            {
                break;
            }
        }
        return chunks;
    }

    private void deleteVectors(Long documentId)
    {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        List<AiKnowledgeChunk> chunks = chunkMapper.selectByDocumentId(documentId);
        if (vectorStore == null || chunks.isEmpty())
        {
            return;
        }
        try
        {
            vectorStore.delete(chunks.stream().map(AiKnowledgeChunk::getVectorId).filter(StringUtils::isNotBlank).toList());
        }
        catch (Exception e)
        {
            log.warn("删除知识库向量失败：documentId={} {}", documentId, e.getMessage());
        }
    }

    private AiKnowledgeIndexJob createJob(Long documentId, String jobType)
    {
        AiKnowledgeIndexJob job = new AiKnowledgeIndexJob();
        job.setDocumentId(documentId);
        job.setJobType(jobType);
        job.setStatus(JOB_RUNNING);
        job.setTotalDocuments(0);
        job.setIndexedDocuments(0);
        job.setFailedDocuments(0);
        job.setMessage("任务已创建");
        jobMapper.insertJob(job);
        return job;
    }

    private void updateProgress(AiKnowledgeIndexJob job, int indexed, int failed, String message)
    {
        job.setIndexedDocuments(indexed);
        job.setFailedDocuments(failed);
        job.setMessage(message);
        jobMapper.updateProgress(job);
    }

    private void finishJob(AiKnowledgeIndexJob job, String status, String message)
    {
        job.setStatus(status);
        job.setMessage(message);
        jobMapper.finishJob(job);
    }

    private AiKnowledgeDocument requireDocument(Long id)
    {
        AiKnowledgeDocument document = documentMapper.selectById(id);
        if (document == null)
        {
            throw new ServiceException("知识库文档不存在");
        }
        return document;
    }

    private List<Path> listSupportedFiles(Path root) throws IOException
    {
        try (Stream<Path> stream = Files.walk(root))
        {
            return stream.filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .sorted()
                    .toList();
        }
    }

    private Path ensureDocsRoot() throws IOException
    {
        Path root = resolveDocsRoot();
        Files.createDirectories(root);
        return root;
    }

    private Path resolveDocsRoot()
    {
        Path configured = Paths.get(ragProperties.getDocsDir());
        if (configured.isAbsolute())
        {
            return configured.normalize();
        }
        return Paths.get(System.getProperty("user.dir")).resolve(configured).normalize();
    }

    private void assertInsideRoot(Path root, Path target)
    {
        if (!target.toAbsolutePath().normalize().startsWith(root.toAbsolutePath().normalize()))
        {
            throw new ServiceException("知识库文件路径非法");
        }
    }

    private boolean isSupportedFile(Path file)
    {
        String extension = extension(file);
        return "md".equals(extension) || "txt".equals(extension);
    }

    private String normalizeInputFileName(String value)
    {
        String fileName = StringUtils.isBlank(value) ? "knowledge.md" : value.trim();
        fileName = fileName.replace('\\', '_').replace('/', '_');
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".md")
                && !fileName.toLowerCase(Locale.ROOT).endsWith(".txt"))
        {
            fileName = fileName + ".md";
        }
        return fileName;
    }

    private Path avoidOverwrite(Path target)
    {
        if (!Files.exists(target))
        {
            return target;
        }
        String fileName = target.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String name = dot < 0 ? fileName : fileName.substring(0, dot);
        String extension = dot < 0 ? "" : fileName.substring(dot);
        return target.getParent().resolve(name + "-" + System.currentTimeMillis() + extension).normalize();
    }

    private String toRelativePath(Path root, Path file)
    {
        return root.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize()).toString()
                .replace('\\', '/');
    }

    private String extension(Path file)
    {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String sha256(Path file) throws Exception
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
    }

    private String extractTitle(String content, String fallback)
    {
        for (String line : content.split("\\R"))
        {
            String trimmed = line.trim();
            if (trimmed.startsWith("#"))
            {
                return trimmed.replaceFirst("^#+", "").trim();
            }
            if (StringUtils.isNotBlank(trimmed))
            {
                return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
            }
        }
        return fallback;
    }

    private String buildVectorId(AiKnowledgeDocument document, int chunkIndex)
    {
        String hash = StringUtils.isBlank(document.getFileHash()) ? "unknown" : document.getFileHash();
        return "cook-kb-" + document.getId() + "-" + chunkIndex + "-" + hash.substring(0, Math.min(12, hash.length()));
    }

    private int estimateTokens(String text)
    {
        return Math.max(1, (text == null ? 0 : text.length()) / 2);
    }

    private AiRagSourceVo toRagSource(Document document)
    {
        Map<String, Object> metadata = document.getMetadata();
        AiRagSourceVo source = new AiRagSourceVo();
        source.setDocumentId(toLong(metadata.get("documentId")));
        source.setChunkId(toLong(metadata.get("chunkId")));
        source.setFileName(String.valueOf(metadata.getOrDefault("fileName", "")));
        source.setTitle(String.valueOf(metadata.getOrDefault("title", "")));
        source.setScore(document.getScore());
        return source;
    }

    private String resolveSourceName(AiRagSourceVo source)
    {
        if (source == null)
        {
            return "本地知识库";
        }
        if (StringUtils.isNotBlank(source.getFileName()))
        {
            return source.getFileName();
        }
        return "本地知识库";
    }

    private Long toLong(Object value)
    {
        if (value instanceof Number number)
        {
            return number.longValue();
        }
        if (value == null || StringUtils.isBlank(String.valueOf(value)))
        {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String shorten(String text, int maxLength)
    {
        if (text == null)
        {
            return "";
        }
        String normalized = text.replace('\n', ' ').trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private AiKnowledgeDocumentVo toDocumentVo(AiKnowledgeDocument document)
    {
        if (document == null)
        {
            return null;
        }
        AiKnowledgeDocumentVo vo = new AiKnowledgeDocumentVo();
        vo.setId(document.getId());
        vo.setFileName(document.getFileName());
        vo.setOriginalName(document.getOriginalName());
        vo.setRelativePath(document.getRelativePath());
        vo.setFileHash(document.getFileHash());
        vo.setFileSize(document.getFileSize());
        vo.setFileType(document.getFileType());
        vo.setTitle(document.getTitle());
        vo.setStatus(document.getStatus());
        vo.setChunkCount(document.getChunkCount());
        vo.setLastIndexedAt(document.getLastIndexedAt());
        vo.setErrorMessage(document.getErrorMessage());
        vo.setCreatedAt(document.getCreatedAt());
        vo.setUpdatedAt(document.getUpdatedAt());
        return vo;
    }

    private AiKnowledgeIndexJobVo toJobVo(AiKnowledgeIndexJob job)
    {
        if (job == null)
        {
            return null;
        }
        AiKnowledgeIndexJobVo vo = new AiKnowledgeIndexJobVo();
        vo.setId(job.getId());
        vo.setDocumentId(job.getDocumentId());
        vo.setJobType(job.getJobType());
        vo.setStatus(job.getStatus());
        vo.setTotalDocuments(job.getTotalDocuments());
        vo.setIndexedDocuments(job.getIndexedDocuments());
        vo.setFailedDocuments(job.getFailedDocuments());
        vo.setMessage(job.getMessage());
        vo.setStartedAt(job.getStartedAt());
        vo.setFinishedAt(job.getFinishedAt());
        vo.setCreatedAt(job.getCreatedAt());
        return vo;
    }

    private int normalizePage(Integer page)
    {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizePageSize(Integer pageSize)
    {
        if (pageSize == null || pageSize < 1)
        {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
