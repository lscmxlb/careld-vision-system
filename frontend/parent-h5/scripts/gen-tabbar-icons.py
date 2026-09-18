#!/usr/bin/env python3
"""生成 uni-app tabBar 图标（81x81 PNG，灰/青两态）"""
from PIL import Image, ImageDraw
import os

SIZE = 81
S = 8  # 超采样倍数
INACTIVE = (148, 163, 184, 255)   # #94a3b8
ACTIVE = (20, 184, 166, 255)      # #14b8a6

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'static', 'tabbar')
OUT = os.path.abspath(OUT)
os.makedirs(OUT, exist_ok=True)


def new_canvas():
    img = Image.new('RGBA', (SIZE * S, SIZE * S), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def rr(draw, box, radius, color, width):
    draw.rounded_rectangle(box, radius=radius, outline=color, width=width)


def icon_child(draw, color):
    """档案：文档（圆角矩形 + 三条内容线）"""
    w = int(6 * S)
    left, top, right, bottom = 15 * S, 9 * S, 66 * S, 72 * S
    rr(draw, (left, top, right, bottom), radius=int(6 * S), color=color, width=w)
    for i, y in enumerate((28, 42, 56)):
        x_start = left + 11 * S
        x_end = right - (24 if i == 2 else 11) * S
        draw.line((x_start, y * S, x_end, y * S), fill=color, width=w)


def icon_appointment(draw, color):
    """预约：日历 + 加号"""
    w = int(6 * S)
    left, top, right, bottom = 11 * S, 15 * S, 70 * S, 70 * S
    rr(draw, (left, top, right, bottom), radius=int(6 * S), color=color, width=w)
    for x in (25 * S, 56 * S):
        draw.line((x, 9 * S, x, 20 * S), fill=color, width=int(6 * S))
    draw.line((left + w // 2, 30 * S, right - w // 2, 30 * S), fill=color, width=w)
    # 加号
    cx, cy, arm = 40 * S, 50 * S, 11 * S
    draw.line((cx - arm, cy, cx + arm, cy), fill=color, width=int(6 * S))
    draw.line((cx, cy - arm, cx, cy + arm), fill=color, width=int(6 * S))


def icon_reserve(draw, color):
    """我的预约：列表（三行：圆点 + 横线）"""
    w = int(6 * S)
    for y in (20, 40, 60):
        draw.ellipse(((16 * S - 4 * S), (y * S - 4 * S), (16 * S + 4 * S), (y * S + 4 * S)), fill=color)
        draw.line((30 * S, y * S, 68 * S, y * S), fill=color, width=w)


def icon_record(draw, color):
    """养护记录：柱状图（三根高度递增的柱子 + 基线）"""
    w = int(6 * S)
    baseline = 66 * S
    for x, height in ((18, 24), (36, 42), (54, 30)):
        draw.line((x * S, baseline, x * S, (66 - height) * S), fill=color, width=int(9 * S))
    draw.line((12 * S, baseline, 70 * S, baseline), fill=color, width=w)


def icon_mine(draw, color):
    """我的：人像（头 + 肩）"""
    w = int(6 * S)
    cx, cy, r = SIZE * S // 2, 27 * S, 15 * S
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), outline=color, width=w)
    draw.arc((13 * S, 44 * S, 68 * S, 96 * S), start=180, end=360, fill=color, width=w)


ICONS = {
    'child': icon_child,
    'appointment': icon_appointment,
    'reserve': icon_reserve,
    'record': icon_record,
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
