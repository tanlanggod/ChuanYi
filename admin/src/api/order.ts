import http from "./http";

export interface OrderItem {
  id?: number;
  orderNo: string;
  orderStatus: string;
  payAmount: number;
  createdAt?: string;
}

export interface OrderListResult {
  list: OrderItem[];
  page: {
    pageNo: number;
    pageSize: number;
    total: number;
  };
}

export async function fetchOrderList(status?: string): Promise<OrderListResult> {
  const response = await http.get("/api/order/list", {
    params: {
      status: status || undefined,
      pageNo: 1,
      pageSize: 20,
    },
  });
  return response.data || { list: [], page: { pageNo: 1, pageSize: 20, total: 0 } };
}
