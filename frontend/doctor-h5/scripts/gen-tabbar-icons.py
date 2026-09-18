#!/usr/bin/env python3
"""生成 uni-app tabBar 图标（81x81 PNG，灰/蓝两态）"""
from PIL import Image, ImageDraw
import os

SIZE = 81
S = 8  # 超采样倍数
INACTIVE = (148, 163, 184, 255)   # #94a3b8
ACTIVE = (37, 99, 235, 255)       # #2563eb

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'static', 'tabbar')
OUT = os.path.abspath(OUT)
os.makedirs(OUT, exist_ok=True)


def new_canvas():
    img = Image.new('RGBA', (SIZE * S, SIZE * S), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def rr(draw, box, radius, color, width):
    draw.rounded_rectangle(box, radius=radius, outline=color, width=width)


def icon_home(draw, color):
    """工作台：2x2 圆角方块"""
    w = int(6.5 * S)
    box = 7 * S
    gap = 8 * S
    inner = (SIZE * S - box * 2 - gap) // 2
    for r in range(2):
        for c in range(2):
            x = box + c * (inner + gap)
            y = box + r * (inner + gap)
            rr(draw, (x, y, x + inner, y + inner), radius=int(4.5 * S), color=color, width=w)


def icon_reserve(draw, color):
    """预约：日历（外框 + 顶部横线 + 挂环 + 日期点）"""
    w = int(6 * S)
    left, top, right, bottom = 11 * S, 15 * S, 70 * S, 70 * S
    rr(draw, (left, top, right, bottom), radius=int(6 * S), color=color, width=w)
    # 挂环
    for x in (25 * S, 56 * S):
        draw.line((x, 9 * S, x, 20 * S), fill=color, width=int(6 * S))
    # 顶部分隔线
    draw.line((left + w // 2, 30 * S, right - w // 2, 30 * S), fill=color, width=w)
    # 日期点
    for r in range(2):
        for c in range(3):
            cx = 24 * S + c * 17 * S
            cy = 43 * S + r * 14 * S
            draw.ellipse((cx - 3 * S, cy - 3 * S, cx + 3 * S, cy + 3 * S), fill=color)


def icon_child(draw, color):
    """档案：文档（圆角矩形 + 三条内容线）"""
    w = int(6 * S)
    left, top, right, bottom = 15 * S, 9 * S, 66 * S, 72 * S
    rr(draw, (left, top, right, bottom), radius=int(6 * S), color=color, width=w)
    for i, y in enumerate((28, 42, 56)):
        x_start = left + 11 * S
        x_end = right - (24 if i == 2 else 11) * S
        draw.line((x_start, y * S, x_end, y * S), fill=color, width=w)


def icon_mine(draw, color):
    """我的：人像（头 + 肩）"""
    w = int(6 * S)
    # 头
    cx, cy, r = SIZE * S // 2, 27 * S, 15 * S
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), outline=color, width=w)
    # 肩
    draw.arc((13 * S, 44 * S, 68 * S, 96 * S), start=180, end=360, fill=color, width=w)


ICONS = {
    'home': icon_home,
    'reserve': icon_reserve,
    'child': icon_child,
    'mine': icon_mine,
}

for name, painter in ICONS.items():
    for suffix, color in (('', INACTIVE), ('-active', ACTIVE)):
        img, draw = new_canvas()
        painter(draw, color)
        img = img.resize((SIZE, SIZE), Image.LANCZOS)
        path = os.path.join(OUT, f'{name}{suffix}.png')
        img.save(path)
        print('written', path)
