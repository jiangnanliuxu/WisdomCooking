package com.ruoyi.cook.common.domain.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVo<T>
{
    private long page;
    private long pageSize;
    private long total;
    private List<T> items;

}
