package com.ruoyi.cook.common.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import com.alibaba.fastjson2.JSON;

/**
 * Long 列表 JSON 字段处理器。
 */
public class JsonLongListTypeHandler extends BaseTypeHandler<List<Long>>
{
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Long> parameter, JdbcType jdbcType)
            throws SQLException
    {
        ps.setString(i, JSON.toJSONString(parameter));
    }

    @Override
    public List<Long> getNullableResult(ResultSet rs, String columnName) throws SQLException
    {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<Long> getNullableResult(ResultSet rs, int columnIndex) throws SQLException
    {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<Long> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException
    {
        return parse(cs.getString(columnIndex));
    }

    private List<Long> parse(String json)
    {
        if (json == null || json.isBlank())
        {
            return Collections.emptyList();
        }
        return JSON.parseArray(json, Long.class);
    }
}
