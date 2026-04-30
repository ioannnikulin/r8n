import type { HttpQueryParams } from "@/lib/http-client";

export type Uuid = string;

export type SortDirectionDto = "ASC" | "DESC";

export interface SortDto {
  direction?: SortDirectionDto;
  property: string;
}

export interface PageRequestDto {
  page: number;
  size: number;
  sort?: readonly SortDto[];
}

export interface PageResponseDto<TItem> {
  items: TItem[];
  page: number;
  size: number;
  total: number;
}

export function createPageQuery(pageable: PageRequestDto): HttpQueryParams {
  const query: HttpQueryParams = {
    page: pageable.page,
    size: pageable.size,
  };

  if (pageable.sort && pageable.sort.length > 0) {
    pageable.sort.forEach(({ direction = "ASC", property }, index) => {
      query[`sort[${index}].property`] = property;
      query[`sort[${index}].direction`] = direction;
    });
  }

  return query;
}
