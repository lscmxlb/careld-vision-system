package com.careld.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分页响应结果
 */
@Data
@Schema(description = "分页响应结果")
public class PageResult<T> {

    @Schema(description = "数据列表")
    private List<T> list;

    @Schema(description = "分页信息")
    private Pagination pagination;

    public PageResult() {
    }

    public PageResult(List<T> list, long page, long size, long total) {
        this.list = list;
        this.pagination = new Pagination(page, size, total);
    }

    @Data
    @Schema(description = "分页信息")
    public static class Pagination {
        @Schema(description = "当前页", example = "1")
        private Long page;

        @Schema(description = "每页大小", example = "20")
        private Long size;

        @Schema(description = "总记录数", example = "100")
        private Long total;

        @Schema(description = "总页数", example = "5")
        private Long pages;

        public Pagination(long page, long size, long total) {
            this.page = page;
            this.size = size;
            this.total = total;
            this.pages = (total + size - 1) / size;
        }
    }

    public static <T> PageResult<T> of(List<T> list, long page, long size, long total) {
        return new PageResult<>(list, page, size, total);
    }
}
