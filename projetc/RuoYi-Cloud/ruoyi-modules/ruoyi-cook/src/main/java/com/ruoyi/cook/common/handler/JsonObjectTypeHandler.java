package com.ruoyi.cook.common.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

/**
 * JSON 对象字段处理器。
 */
public class JsonObjectTypeHandler extends BaseTypeHandler<Map<String, Object>>
{
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType)
            throws SQLException
    {
        ps.setString(i, JSON.toJSONString(parameter));
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException
    {
        return parse(rs.getString(columnName));
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException
    {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException
    {
        return parse(cs.getString(columnIndex));
    }

    private Map<String, Object> parse(String json)
    {
        if (json == null || json.isBlank())
        {
            return Collections.emptyMap();
        }
        return JSON.parseObject(json, new TypeReference<Map<String, Object>>()
        {
        });
    }
}
