#!/usr/bin/env python3
"""Generate Compose ImageVectors from pinned Ionicons SVG geometry (MIT)."""
import argparse
import concurrent.futures
import hashlib
import json
from pathlib import Path
import re
import urllib.request
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
REVISION = 'd1e2c48641fd5f4910ee42a144dc1c84b1a9a4ee'
SOURCE = f'https://raw.githubusercontent.com/ionic-team/ionicons/{REVISION}'
ASSETS = ROOT / 'licenses/ionicons'
OUTPUT = ROOT / 'app/src/main/kotlin/com/hermesandroid/relay/ui/icons/RelayIcons.kt'
# Semantic names retain their existing call-site meaning; geometry is upstream Ionicons.
ICONS = {
    'AccessTime':'time-outline', 'AccountTree':'git-network-outline', 'Add':'add-outline',
    'AddComment':'chatbubble-ellipses-outline', 'Analytics':'bar-chart-outline', 'Archive':'archive-outline',
    'ArrowBack':'chevron-back-outline', 'AutoAwesome':'extension-puzzle-outline', 'Bolt':'flash-outline',
    'CenterFocusStrong':'scan-outline', 'Chat':'chatbubble-outline', 'ChatBubble':'chatbubble-outline',
    'Check':'checkmark-outline', 'CheckCircle':'checkmark-circle', 'ChevronRight':'chevron-forward-outline',
    'Close':'close-outline', 'Code':'terminal-outline', 'ContentCopy':'copy-outline',
    'ContentPaste':'clipboard-outline', 'Dashboard':'grid-outline', 'Delete':'trash-outline',
    'Description':'document-text-outline', 'Devices':'hardware-chip-outline', 'Dns':'server-outline',
    'Edit':'create-outline', 'ErrorOutline':'alert-circle-outline', 'ExpandLess':'chevron-up-outline',
    'ExpandMore':'chevron-down-outline', 'Extension':'extension-puzzle-outline', 'FilterList':'filter-outline',
    'Folder':'folder-outline', 'FormatQuote':'chatbox-ellipses-outline', 'GraphicEq':'mic-outline',
    'Groups':'people-outline', 'Home':'home-outline', 'Image':'image-outline',
    'Info':'information-circle-outline', 'InsertDriveFile':'document-outline', 'Key':'key-outline',
    'KeyboardArrowDown':'chevron-down-outline', 'KeyboardArrowRight':'chevron-forward-outline',
    'KeyboardArrowUp':'chevron-up-outline', 'Language':'globe-outline', 'Link':'link-outline',
    'Lock':'lock-closed-outline', 'Menu':'menu-outline', 'Message':'chatbox-outline', 'Mic':'mic-outline',
    'MoreHoriz':'ellipsis-horizontal', 'MoreVert':'ellipsis-horizontal', 'NewReleases':'newspaper-outline',
    'Notifications':'notifications-outline', 'Palette':'color-palette-outline', 'Person':'person-outline',
    'PhoneAndroid':'phone-portrait-outline', 'PhotoCamera':'camera-outline', 'PhotoLibrary':'images-outline',
    'Psychology':'person-circle-outline', 'QrCodeScanner':'qr-code-outline',
    'RadioButtonUnchecked':'ellipse-outline', 'Refresh':'refresh-outline', 'Schedule':'time-outline',
    'Search':'search-outline', 'Security':'shield-checkmark-outline', 'Send':'arrow-up-outline',
    'Settings':'settings-outline', 'Share':'share-outline', 'Star':'star', 'StarBorder':'star-outline',
    'Stop':'stop', 'Tune':'options-outline', 'ViewInAr':'cube-outline', 'Visibility':'eye-outline',
    'VisibilityOff':'eye-off-outline', 'VolumeUp':'volume-high-outline', 'Warning':'warning-outline',
    'ChatSelected':'chatbubble', 'ManageSelected':'grid', 'ConnectionsSelected':'link',
    'Briefcase':'briefcase', 'Personal':'person', 'Research':'library',
}
MIRRORED = {'ArrowBack', 'ChevronRight', 'KeyboardArrowRight', 'Share'}


def fetch(name):
    path = ASSETS / 'svg' / f'{name}.svg'
    if not path.exists():
        with urllib.request.urlopen(f'{SOURCE}/src/svg/{name}.svg', timeout=30) as response:
            path.write_bytes(response.read())


def number(value):
    return f'{float(value):g}'


def geometry(node):
    tag = node.tag.split('}')[-1]
    a = node.attrib
    if tag == 'path':
        return a['d']
    if tag == 'line':
        return f'M{a["x1"]},{a["y1"]} L{a["x2"]},{a["y2"]}'
    if tag in {'polyline', 'polygon'}:
        return 'M' + a['points'] + ('Z' if tag == 'polygon' else '')
    if tag in {'circle', 'ellipse'}:
        x, y = float(a.get('cx',0)), float(a.get('cy',0))
        rx, ry = float(a.get('rx',a.get('r',0))), float(a.get('ry',a.get('r',0)))
        return f'M{x-rx},{y} a{rx},{ry} 0 1,0 {2*rx},0 a{rx},{ry} 0 1,0 {-2*rx},0Z'
    if tag == 'rect':
        x,y,w,h = (float(a.get(k,0)) for k in ['x','y','width','height'])
        rx,ry = min(float(a.get('rx',a.get('ry',0))),w/2),min(float(a.get('ry',a.get('rx',0))),h/2)
        if not rx or not ry:
            return f'M{x},{y} h{w} v{h} h{-w}Z'
        return (f'M{x+rx},{y} H{x+w-rx} A{rx},{ry} 0 0,1 {x+w},{y+ry} V{y+h-ry} '
                f'A{rx},{ry} 0 0,1 {x+w-rx},{y+h} H{x+rx} A{rx},{ry} 0 0,1 {x},{y+h-ry} '
                f'V{y+ry} A{rx},{ry} 0 0,1 {x+rx},{y}Z')
    raise ValueError(f'Unsupported shape {tag}')


def vector(name, raw):
    svg = ET.fromstring(raw)
    assert svg.attrib['viewBox'] == '0 0 512 512'
    rows = [f'    val {name}: ImageVector by lazy {{',
            f'        ImageVector.Builder("Ionicons.{name}", 24.dp, 24.dp, 512f, 512f,',
            f'            autoMirror = {str(name in MIRRORED).lower()},', '        ).apply {']
    for node in svg:
        a = node.attrib
        assert 'transform' not in a, (name, a)
        style = {k.strip():v.strip() for k,v in (item.split(':',1) for item in a.get('style','').split(';') if item)}
        def attr(key, default): return style.get(key, a.get(key, default))
        def paint(key, default):
            value = attr(key,default)
            assert value in {'none','#000','#000000','currentColor','black'}, (name,key,value)
            return 'null' if value=='none' else 'SolidColor(Color.Black)'
        cap = {'round':'Round','butt':'Butt','square':'Square'}[attr('stroke-linecap','butt')]
        join = {'round':'Round','miter':'Miter','bevel':'Bevel'}[attr('stroke-linejoin','miter')]
        rule = {'nonzero':'NonZero','evenodd':'EvenOdd'}[attr('fill-rule','nonzero')]
        rows += ['            addPath(',
                 f'                pathData = PathParser().parsePathString("{geometry(node)}").toNodes(),',
                 f'                fill = {paint("fill","black")}, stroke = {paint("stroke","none")},',
                 f'                strokeLineWidth = {number(attr("stroke-width","1").removesuffix("px"))}f,',
                 f'                strokeLineCap = StrokeCap.{cap}, strokeLineJoin = StrokeJoin.{join},',
                 f'                strokeLineMiter = {number(attr("stroke-miterlimit","4"))}f,',
                 f'                pathFillType = PathFillType.{rule},', '            )']
    rows += ['        }.build()', '    }']
    return '\n'.join(rows)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fetch', action='store_true', help='Download missing pinned upstream SVGs')
    parser.add_argument('--check', action='store_true', help='Verify checked-in output without writes')
    args=parser.parse_args()
    if args.fetch:
        (ASSETS/'svg').mkdir(parents=True,exist_ok=True)
        with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
            list(pool.map(fetch, sorted(set(ICONS.values()))))
        with urllib.request.urlopen(SOURCE+'/LICENSE',timeout=20) as response:
            (ASSETS/'LICENSE').write_bytes(response.read())
    assets = {n:(ASSETS/'svg'/f'{n}.svg').read_bytes() for n in sorted(set(ICONS.values()))}
    manifest = json.dumps({'source':'https://github.com/ionic-team/ionicons','revision':REVISION,'license':'MIT',
                          'mapping':ICONS,'sha256':{n+'.svg':hashlib.sha256(b).hexdigest() for n,b in assets.items()}},indent=2)+'\n'
    header = '''// Generated by scripts/generate-ionicons.py. Do not edit geometry by hand.
// Ionicons (MIT), pinned source and original SVGs: licenses/ionicons/.
package com.hermesandroid.relay.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Local Compose vectors; no icon font, runtime parser downloads, or external requests. */
object RelayIcons {
'''
    output = header + '\n\n'.join(vector(n,assets[src]) for n,src in ICONS.items())+'\n}\n'
    for path, content in [(OUTPUT,output),(ASSETS/'manifest.json',manifest)]:
        if args.check: assert path.read_text()==content, f'{path} is out of date'
        else: path.write_text(content)
    print(f'{len(ICONS)} Compose symbols from {len(assets)} original Ionicons SVGs: OK')

if __name__=='__main__':
    main()
