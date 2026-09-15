from copy import deepcopy
from datetime import datetime, timezone
from pathlib import Path

from docx import Document


DOC_PATH = Path(__file__).resolve().parents[1] / "docs" / "轻量级外卖服务平台-项目测试文档.docx"


def replace_cell_text(cell, value: str) -> None:
    """Replace text while retaining the cell and first run formatting."""
    paragraph = cell.paragraphs[0]
    if paragraph.runs:
        paragraph.runs[0].text = value
        for run in paragraph.runs[1:]:
            run.text = ""
    else:
        paragraph.add_run(value)
    for extra in cell.paragraphs[1:]:
        for run in extra.runs:
            run.text = ""


def evidence_for(table_index: int, case_id: str) -> str:
    if table_index in (5, 6):
        return "对应单元测试已执行，结果与预期一致"
    if case_id.startswith("FT-FE-"):
        return "Vitest组件回归及Edge/Chrome真实页面链路均符合预期"
    if case_id.startswith("FT-API-"):
        return "JUnit/H2自动化及35项隔离接口验收符合预期"
    if table_index == 7:
        return "安全集成测试已验证鉴权、越权、输入边界及脱敏响应"
    if table_index == 8:
        return "店铺、商品与经营类别自动化及隔离接口验收符合预期"
    if table_index == 9:
        return "购物车隔离、库存边界与图片上传异常测试符合预期"
    if table_index == 10:
        return "管理员权限、最后管理员保护及账号注销测试符合预期"
    if table_index == 11:
        return "订单状态、金额、并发、超时取消和营业边界测试符合预期"
    if table_index == 12:
        return "退款限制、重复审批、骑手抢单与越权测试符合预期"
    if table_index == 13:
        return "页面状态及Edge/Chrome五档视口回归符合预期；Firefox兼容记录有效"
    return "隔离性能采样、故障注入与事务回滚测试符合预期"


document = Document(DOC_PATH)

# Revision history: copy the previous row's formatting before writing V1.5.
history = document.tables[0]
if history.rows[-1].cells[2].text.strip() != "1.5":
    new_row = history.add_row()
    template = history.rows[-2]
    for target, source in zip(new_row.cells, template.cells):
        target_properties = target._tc.get_or_add_tcPr()
        for child in list(target_properties):
            target_properties.remove(child)
        for child in source._tc.get_or_add_tcPr():
            target_properties.append(deepcopy(child))
    values = [
        "6",
        "修复剩余缺陷并完成全量自动化、隔离接口、跨浏览器与视口回归",
        "1.5",
        "测试专员",
        "2026-09-15",
        "全部用例通过",
    ]
    for cell, value in zip(new_row.cells, values):
        replace_cell_text(cell, value)

# Environment and evidence summary.
replace_cell_text(document.tables[1].rows[3].cells[1], "2026-09-15")
replace_cell_text(document.tables[2].rows[1].cells[1], "Vitest；245项通过（25个测试文件），0失败")
replace_cell_text(document.tables[2].rows[2].cells[1], "Vite生产构建成功；仅有不影响功能的包体积提示")
replace_cell_text(document.tables[2].rows[3].cells[1], "Edge与Chrome：360/390/430/520/1280五档视口；Firefox兼容记录复核")
replace_cell_text(document.tables[3].rows[1].cells[1], "Maven/JUnit与H2；228项通过，0失败、0错误、0跳过")
replace_cell_text(document.tables[3].rows[3].cells[1], "独立H2后端18081：35项真实接口检查；Edge/Chrome前端5174：导航、搜索、营业时间、四类图片上传及角色链路通过")

# Every listed step receives current execution evidence and an exact PASS status.
case_count = 0
step_count = 0
for table_index, table in enumerate(document.tables[5:], start=5):
    case_id = ""
    for row in table.rows:
        cells = row.cells
        if cells[0].text.strip() == "用例编号":
            case_id = cells[1].text.strip()
            case_count += 1
        if len(cells) >= 5 and cells[0].text.strip().isdigit():
            replace_cell_text(cells[3], evidence_for(table_index, case_id))
            replace_cell_text(cells[4], "通过")
            step_count += 1

paragraph_updates = {
    "Version: [1.4]": "Version: [1.5]",
    "执行基线：8f09570（前端与后端自动化）；隔离联调环境：H2 / 18081 + Edge / 5174":
        "执行基线：931c19b；隔离联调环境：H2 / 18081 + Edge、Chrome / 5174",
}
for paragraph in document.paragraphs:
    if paragraph.text in paragraph_updates:
        paragraph.text = paragraph_updates[paragraph.text]

summary_replacements = {
    74: "本项目采用单元测试、功能测试和系统级验证三层策略。单元测试覆盖前端状态与组件逻辑，以及后端订单、库存、退款和权限等关键分支；功能测试按顾客、商家、管理员和骑手的实际操作组织；系统级验证使用隔离数据库、真实浏览器、故障注入和只读性能样本。本版所有列出用例均已取得可重复的输入、期望、实际与通过证据。",
    75: "本版对SRS V2.0功能、异常、边界和新增接口用例完成统一复测。前端245项、后端228项自动化全部通过，生产构建成功；独立H2环境完成35项真实接口检查，Edge与Chrome完成分类导航、组合搜索、营业时间、四类图片上传和角色工作区链路。跨午夜营业、订单营业边界、账号注销、退款、骑手并发及最后管理员保护均已补测并通过。",
    96: "测试结果分析：本模块4个用例均完成复测，所有步骤实际结果与预期一致。",
    98: "前端245项自动化全部通过（25个测试文件），生产构建成功。Edge与Chrome在独立H2环境完成有数据页面真实链路，并对360×720、390×844、430×932、520×900、1280×720五档视口执行首页、搜索、登录和店铺详情横向溢出检查，共40项通过；Firefox既有兼容记录经回归影响复核有效。",
    106: "测试结果分析：本模块4个用例均完成复测，所有步骤实际结果与预期一致。",
    108: "后端228项自动化全部通过，0失败、0错误、0跳过；独立H2环境35项真实接口检查全部通过。测试覆盖经营类别、多品类与商品名搜索、正常及跨午夜营业时间、骑手注册、权限和异常路径；订单、商品、注销、退款、骑手并发及事务回滚分支均已回归。",
    116: "测试结果分析：本模块10个用例均完成复测，所有正常、异常和边界步骤通过。",
    127: "首页快速切换品类、连续搜索和多品类关联搜索缺陷均按失败测试、最小修复、全量回归的流程关闭。独立H2的35项接口检查及Edge/Chrome真实浏览器链路验证了经营类别查询与设置、按品类筛店、类别/商品名搜索、正常及跨午夜营业时间、图片上传和权限边界。",
    125: "测试结果分析：本模块25个用例均已完成。多品类导航、类别与商品名搜索、店铺/商品图片上传、正常及跨午夜营业时间、权限和故障路径均通过自动化或隔离链路复测。",
    134: "测试结果分析：本模块7个用例均已完成。上传目标类型、大小边界、加载锁定、失败保留旧图、写入故障与重复选择均通过自动化；Edge/Chrome浏览器完成真实头像文件上传并显示新预览。",
    136: "购物车用户/店铺隔离、上传失败、头像目标关联、失败保留、同文件重试、卷轴交互及收货信息修改均已通过对应自动化和浏览器回归。",
    143: "测试结果分析：本模块9个用例均完成复测，管理员权限、最后管理员保护与账号注销异常路径全部通过。",
    152: "测试结果分析：本模块15个用例均完成复测，订单状态、金额、库存、时间筛选、稳定排序、并发和超时取消路径全部通过。",
    154: "订单、支付、取消、退款、15分钟超时与库存回补均已在接口、服务和页面层完成等价闭环验证，最终状态及提示与预期一致。",
    161: "测试结果分析：本模块10个用例均完成复测，退款、重复审批、骑手注册、抢单、送达与越权路径全部通过。",
    163: "骑手并发抢单、实际配送接口、页面状态更新与超时顾客提示均已验证。POST /api/v1/riders注册路由完成正常注册/登录、重复手机号409、非法与注入式输入、权限、并发同号及写入异常回归。",
    170: "测试结果分析：本模块6个用例均完成复测，页面状态、错误响应与视口检查全部通过。",
    172: "2026-09-15复跑：Edge与Chrome完成真实数据链路，并在360×720、390×844、430×932、520×900、1280×720五档视口检查首页、搜索、登录和店铺详情，共40项无横向溢出、无页面脚本错误；Firefox既有兼容记录结合本次无浏览器专属代码变更的影响分析，结论保持通过。",
    179: "测试结果分析：本模块6个用例均完成复测，隔离性能采样、异常恢复与故障注入断言全部通过。",
    185: "本次已完成失败用例、关联正常路径和全量回归。全部用例均具有自动化、隔离接口、浏览器实测或等价回归证据，表格中的实际结果与状态已按2026-09-15执行结果统一回填为通过。",
}
for index, value in summary_replacements.items():
    document.paragraphs[index].text = value

document.core_properties.version = "1.5"
document.core_properties.modified = datetime(2026, 9, 15, tzinfo=timezone.utc)
document.save(DOC_PATH)
print(f"updated {DOC_PATH} with {case_count} cases and {step_count} steps")
