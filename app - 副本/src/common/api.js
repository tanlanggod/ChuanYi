import { request } from "./http";

export function fetchBannerList() {
  return request({ url: "/content/banner/list" });
}

export function fetchGallery(pageNo = 1, pageSize = 10, sortType = "PUBLISH_TIME") {
  return request({
    url: "/gallery/list",
    data: { pageNo, pageSize, sortType },
  });
}

export function fetchGalleryDetail(id) {
  return request({
    url: `/gallery/${id}`,
  });
}

export function publishGallery(payload) {
  return request({
    url: "/gallery/publish",
    method: "POST",
    data: payload || {},
  });
}

export function likeGallery(id) {
  return request({
    url: `/gallery/${id}/like`,
    method: "POST",
  });
}

export function unlikeGallery(id) {
  return request({
    url: `/gallery/${id}/like`,
    method: "DELETE",
  });
}

export function fetchGoodsCategories(withCount = true) {
  return request({
    url: "/goods/categories",
    data: { withCount },
  });
}

export function fetchGoodsList(params = {}) {
  return request({
    url: "/goods/list",
    data: Object.assign({ pageNo: 1, pageSize: 20 }, params),
  });
}

export function fetchGoodsDetail(spuId) {
  return request({
    url: `/goods/${spuId}`,
  });
}

export function fetchCartDetail() {
  return request({ url: "/cart" });
}

export function addCartItem(payload) {
  return request({
    url: "/cart/items",
    method: "POST",
    data: payload,
  });
}

export function updateCartItemQty(cartItemId, qty) {
  return request({
    url: `/cart/items/${cartItemId}`,
    method: "PUT",
    data: { qty },
  });
}

export function deleteCartItem(cartItemId) {
  return request({
    url: `/cart/items/${cartItemId}`,
    method: "DELETE",
  });
}

export function selectCartItem(cartItemId, selected) {
  return request({
    url: `/cart/items/${cartItemId}/select`,
    method: "PUT",
    data: { selected },
  });
}

export function selectAllCartItems(selected) {
  return request({
    url: "/cart/items/select-all",
    method: "POST",
    data: { selected },
  });
}

export function previewCartSettlement(cartItemIds = []) {
  return request({
    url: "/cart/settlement/preview",
    method: "POST",
    data: { cartItemIds },
  });
}

export function fetchAddressList() {
  return request({ url: "/address/list" });
}

export function saveAddress(payload) {
  return request({
    url: "/address/save",
    method: "POST",
    data: payload,
  });
}

export function deleteAddress(id) {
  return request({
    url: `/address/${id}`,
    method: "DELETE",
  });
}

export function fetchOrderList(status = "", pageNo = 1, pageSize = 20) {
  return request({
    url: "/order/list",
    data: {
      status: status || undefined,
      pageNo,
      pageSize,
    },
  });
}

export function fetchOrderDetail(orderNo) {
  return request({
    url: `/order/${orderNo}`,
  });
}

export function cancelOrder(orderNo, remark = "") {
  return request({
    url: `/order/${orderNo}/cancel`,
    method: "POST",
    data: { remark },
  });
}

export function createOrderFromSnapshot(payload) {
  return request({
    url: "/order/create",
    method: "POST",
    data: payload,
  });
}

export function createOrderFromCart(payload) {
  return request({
    url: "/order/create-from-cart",
    method: "POST",
    data: payload,
  });
}

export function createOrderDirectGoods(payload) {
  return request({
    url: "/order/create-direct-goods",
    method: "POST",
    data: payload,
  });
}

export function confirmReceipt(orderNo, remark = "") {
  return request({
    url: `/order/${orderNo}/confirm-receipt`,
    method: "POST",
    data: { remark },
  });
}

export function fetchLogisticsTrack(carrierCode, trackingNo) {
  return request({
    url: "/logistics/track",
    data: { carrierCode, trackingNo },
  });
}

export function fetchDraftList(pageNo = 1, pageSize = 20) {
  return request({
    url: "/design/draft/list",
    data: { pageNo, pageSize },
  });
}

export function fetchDraftDetail(id) {
  return request({
    url: `/design/draft/${id}`,
  });
}

export function saveDraft(payload) {
  return request({
    url: "/design/draft/save",
    method: "POST",
    data: payload,
  });
}

export function deleteDraft(id) {
  return request({
    url: `/design/draft/${id}`,
    method: "DELETE",
  });
}

export function createSnapshotFromDraft(draftId) {
  return request({
    url: "/design/snapshot/create-from-draft",
    method: "POST",
    data: { draftId },
  });
}

export function fetchSnapshotDetail(snapshotNo) {
  return request({
    url: `/design/snapshot/${snapshotNo}`,
  });
}

export function fetchSkuCategories() {
  return request({ url: "/sku/categories" });
}

export function fetchSkuList(params = {}) {
  return request({
    url: "/sku/list",
    data: Object.assign({ pageNo: 1, pageSize: 20 }, params),
  });
}

export function mockPay(orderNo) {
  return request({
    url: "/payment/mock/pay",
    method: "POST",
    data: { orderNo },
  });
}

export function mockPayCallback(payNo, orderNo, payStatus = "SUCCESS") {
  return request({
    url: "/payment/mock/callback",
    method: "POST",
    data: { payNo, orderNo, payStatus },
  });
}
