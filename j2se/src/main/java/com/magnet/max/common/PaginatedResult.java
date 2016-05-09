package com.magnet.max.common;

import java.util.List;

public class PaginatedResult {

    private List result;
    private int currentOffset;
    private long totalCount;

    public PaginatedResult( List result, int currentOffset,long totalCount) {
        this.currentOffset = currentOffset;
        this.result = result;
        this.totalCount = totalCount;
    }

    public int getCurrentOffset() {
        return currentOffset;
    }

    public List getResult() {
        return result;
    }

    public long getTotalCount() {
        return totalCount;
    }

}
