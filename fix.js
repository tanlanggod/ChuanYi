const fs = require('fs');
let content = fs.readFileSync('D:\\IdeaProjects\\ChuanYi\\app - 副本\\src\\pages-diy\\editor.vue', 'utf8');

content = content.replace(/Total \{\{ formatAmount\(totalPrice\) \}\} CNY/g, '总计 {{ formatAmount(totalPrice) }} 元');
content = content.replace(/My Design/g, '我的设计');
content = content.replace(/MineStone/g, '养个石头');
content = content.replace(/>Title</g, '>设计名称<');
content = content.replace(/placeholder="我的设计"/g, 'placeholder="例如: 翠竹流云"');
content = content.replace(/>Wrist\(cm\)</g, '>净手围(cm)<');
content = content.replace(/placeholder="13"/g, 'placeholder="默认 13"');
content = content.replace(/>Clear</g, '>清空珠子<');
content = content.replace(/>Save</g, '>保存草稿<');
content = content.replace(/>Finish</g, '>完成设计<');

content = content.replace(/label: "In Use"/g, 'label: "已选"');
content = content.replace(/label: "Bead"/g, 'label: "散珠"');
content = content.replace(/label: "Spacer"/g, 'label: "隔珠"');
content = content.replace(/label: "Other"/g, 'label: "其他"');
content = content.replace(/label: "Pendant"/g, 'label: "吊坠"');

content = content.replace(/Search materials\.\.\./g, '搜索散珠/配件...');
content = content.replace(/>Search</g, '>搜索<');
content = content.replace(/No selected materials/g, '当前未选择任何材料');
content = content.replace(/Selected Item/g, '已选商品');
content = content.replace(/CNY \{\{ formatAmount\(sku.price\) \}\}/g, '￥{{ formatAmount(sku.price) }}');
content = content.replace(/>Remove</g, '>移除<');
content = content.replace(/No category in this type/g, '该分类下暂无商品');
content = content.replace(/No materials/g, '未搜索到相关材料');
content = content.replace(/>Add</g, '>穿入<');
content = content.replace(/Selected \{\{ selectedSkus.length \}\} items/g, '已选中 {{ selectedSkus.length }} 颗珠材');
content = content.replace(/>Manage selected</g, '>管理已选<');

content = content.replace(/Init failed/g, '初始化失败');
content = content.replace(/Save failed/g, '保存失败');
content = content.replace(/Draft saved/g, '草稿已保存');
content = content.replace(/Snapshot create failed/g, '快照生成失败');
content = content.replace(/Finish failed/g, '完成设计失败');

content = content.replace(/>Notice</g, '>小贴士<');
content = content.replace(/Wrist -/g, '手围 -');

content = content.replace(/<view class=\"muted\">养个石头<\/view>/g, '<view class=\"muted\">设计台<\/view>');

fs.writeFileSync('D:\\IdeaProjects\\ChuanYi\\app - 副本\\src\\pages-diy\\editor.vue', content, 'utf8');
console.log("Done");