package cn.edu.tju.takeout.recommend;

import cn.edu.tju.takeout.shop.Shop;

/** 打分阶段在「召回 → 打分 → 重排」之间传递的中间结果 */
record ScoredShop(Shop shop, double score) {}
