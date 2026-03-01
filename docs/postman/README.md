# Postman

Files:
- `ChuanYi.postman_collection.json`
- `ChuanYi.local.postman_environment.json`

Quick start:
1. Import both files into Postman.
2. Select environment `ChuanYi Local`.
3. Run `Auth / Login PHONE (seed user)` first to set `token`.
4. Run requests in `DIY Order Flow` or `Goods Cart Flow`.
5. In `DIY Order Flow`, run `Mock Pay Callback SUCCESS -> Ship Order -> Logistics Track By TrackingNo -> Confirm Receipt` to test fulfillment.
6. For admin endpoints, run `Admin Console / Admin Login` first to set `adminToken`.

Default seed values used by this collection:
- `phone=15700000000`
- `draftId=1`
- `addressId=1`
- `spuId=20001`
- `skuId=30001`
- `adminUsername=admin`
- `adminPassword=123456`
- `adminSkuId=30001`
