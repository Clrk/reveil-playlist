"""Macaron aube : une seule description -> SVG (aperçu) + VectorDrawable Android."""
import math, os, sys
def circ(cx,cy,r): return f'M{cx-r:.2f},{cy:.2f}a{r:.2f},{r:.2f} 0 1,0 {2*r:.2f},0a{r:.2f},{r:.2f} 0 1,0 {-2*r:.2f},0Z'
def rect(x,y,w,h): return f'M{x},{y}h{w}v{h}h{-w}Z'
def halfup(cx,cy,r): return f'M{cx-r:.2f},{cy:.2f}a{r:.2f},{r:.2f} 0 0,1 {2*r:.2f},0Z'
def wedge(cx,cy,r0,r1,a0,a1):
    a0,a1=math.radians(a0),math.radians(a1); p=lambda r,a:(cx+r*math.cos(a),cy+r*math.sin(a))
    (x0,y0),(x1,y1),(x2,y2),(x3,y3)=p(r0,a0),p(r1,a0),p(r1,a1),p(r0,a1)
    return f'M{x0:.2f},{y0:.2f}L{x1:.2f},{y1:.2f}A{r1},{r1} 0 0,1 {x2:.2f},{y2:.2f}L{x3:.2f},{y3:.2f}A{r0},{r0} 0 0,0 {x0:.2f},{y0:.2f}Z'
# fill: '#RRGGBB' | ('lin',x1,y1,x2,y2,[(off,col,alpha)]) | ('rad',cx,cy,r,[...])
def F(path,fill,alpha=1): return dict(k='fill',d=path,fill=fill,a=alpha)
def S(path,col,alpha,w): return dict(k='stroke',d=path,col=col,a=alpha,w=w)
def CLIP(path,children): return dict(k='clip',d=path,ch=children)

CX=CY=54; R=31; RL=17; HZ=57.5
def grooves():
    by={}
    r=RL+1.5;i=0
    while r<=R-0.6:
        o=[.045,.06,.075][(i*7)%3]; by.setdefault(o,[]).append(circ(CX,CY,r)); r+=1.0; i+=1
    return [S(''.join(v),'#EADBC8',o,.35) for o,v in by.items()]
def sheen():
    out=[]
    for a in (-120,60):
        for w,o in ((16,.025),(10,.03),(5,.04),(2,.05)): out.append(F(wedge(CX,CY,RL+.5,R-.5,a-w,a+w),'#EADBC8',o))
    return out
BG=[F(rect(0,0,108,108),('rad',54,45.4,81,[(0,'#141B23',1),(1,'#05080A',1)]))]
FG=[F(circ(CX,CY,R),('rad',49.5,47.4,22,[(0,'#1C2025',1),(1,'#08090B',1)]))]+grooves()+sheen()+[
  CLIP(circ(CX,CY,RL),[
    F(rect(36,36,36,36),('lin',0,37,0,71,[(0,'#26304A',1),(.36,'#8C6A82',1),(.55,'#E7A07E',1),(.6,'#F4BE7C',1)])),
    F(circ(CX,HZ,15),('rad',CX,HZ,15,[(0,'#F6C68A',.55),(1,'#F6C68A',0)])),
    F(circ(CX,HZ,7.6),('rad',CX,HZ-1.5,8,[(0,'#FFE6B8',1),(1,'#F2B36C',1)])),
    F(rect(36,HZ,36,16),'#2A1E22'),
    F(rect(36,HZ,36,.7),'#FFD9A8',.9),
    S(f'M41,{HZ+3.2}H67M44,{HZ+6.2}H64M47.5,{HZ+9.2}H60.5','#F2B36C',.4,.6),
  ]),
  S(circ(CX,CY,RL),'#0A0C0E',1,.8),
  F(circ(CX,CY,.95),'#7A4A2C'),
  S(circ(CX,CY,R-.3),'#EADBC8',.16,.6),
]
MONO=[F(circ(CX,CY,30.5)+circ(CX,CY,RL),'#000000'),  # evenodd handled below
      F(halfup(CX,HZ,8),'#000000'),F(rect(40,HZ+1.2,28,1.8),'#000000'),F(rect(45,HZ+5,18,1.6),'#000000')]

# ---------- SVG ----------
gid=[0]
def svg_fill(fill,defs):
    if isinstance(fill,str): return fill
    gid[0]+=1; i=f'g{gid[0]}'
    stops=''.join(f'<stop offset="{o}" stop-color="{c}" stop-opacity="{a}"/>' for o,c,a in fill[-1])
    if fill[0]=='lin': defs.append(f'<linearGradient id="{i}" gradientUnits="userSpaceOnUse" x1="{fill[1]}" y1="{fill[2]}" x2="{fill[3]}" y2="{fill[4]}">{stops}</linearGradient>')
    else: defs.append(f'<radialGradient id="{i}" gradientUnits="userSpaceOnUse" cx="{fill[1]}" cy="{fill[2]}" r="{fill[3]}">{stops}</radialGradient>')
    return f'url(#{i})'
def to_svg(els,evenodd=False):
    defs=[]
    def emit(e):
        if e['k']=='fill': return f'<path d="{e["d"]}" fill="{svg_fill(e["fill"],defs)}" fill-opacity="{e["a"]}"{" fill-rule=\"evenodd\"" if evenodd else ""}/>'
        if e['k']=='stroke': return f'<path d="{e["d"]}" fill="none" stroke="{e["col"]}" stroke-opacity="{e["a"]}" stroke-width="{e["w"]}"/>'
        gid[0]+=1; i=f'c{gid[0]}'; defs.append(f'<clipPath id="{i}"><path d="{e["d"]}"/></clipPath>')
        return f'<g clip-path="url(#{i})">'+''.join(emit(c) for c in e['ch'])+'</g>'
    body=''.join(emit(e) for e in els)
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108"><defs>{"".join(defs)}</defs>{body}</svg>'

# ---------- VectorDrawable ----------
def argb(c,a): return f'#{round(a*255):02X}{c[1:]}'
def vd_fill(fill,a):
    if isinstance(fill,str): return f' android:fillColor="{fill}"'+(f' android:fillAlpha="{a}"' if a!=1 else ''), ''
    items=''.join(f'<item android:offset="{o}" android:color="{argb(c,al)}"/>' for o,c,al in fill[-1])
    if fill[0]=='lin': g=f'<gradient android:type="linear" android:startX="{fill[1]}" android:startY="{fill[2]}" android:endX="{fill[3]}" android:endY="{fill[4]}">{items}</gradient>'
    else: g=f'<gradient android:type="radial" android:centerX="{fill[1]}" android:centerY="{fill[2]}" android:gradientRadius="{fill[3]}">{items}</gradient>'
    return (f' android:fillAlpha="{a}"' if a!=1 else ''), f'<aapt:attr name="android:fillColor">{g}</aapt:attr>'
def to_vd(els,evenodd=False):
    def emit(e,ind='    '):
        if e['k']=='fill':
            attrs,inner=vd_fill(e['fill'],e['a'])
            ft=' android:fillType="evenOdd"' if evenodd else ''
            return f'{ind}<path android:pathData="{e["d"]}"{attrs}{ft}' + (f'>\n{ind}    {inner}\n{ind}</path>\n' if inner else '/>\n')
        if e['k']=='stroke':
            return f'{ind}<path android:pathData="{e["d"]}" android:strokeColor="{e["col"]}" android:strokeAlpha="{e["a"]}" android:strokeWidth="{e["w"]}"/>\n'
        return f'{ind}<group>\n{ind}    <clip-path android:pathData="{e["d"]}"/>\n'+''.join(emit(c,ind+'    ') for c in e['ch'])+f'{ind}</group>\n'
    return ('<?xml version="1.0" encoding="utf-8"?>\n<!-- Généré par tools/icon/icon.py : ne pas modifier à la main. -->\n'
      '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    xmlns:aapt="http://schemas.android.com/aapt"\n'
      '    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n'
      + ''.join(emit(e) for e in els) + '</vector>\n')

if __name__=='__main__':
    out=sys.argv[1] if len(sys.argv)>1 else '.'
    res=sys.argv[2] if len(sys.argv)>2 else None
    os.makedirs(out,exist_ok=True)
    for n,els,eo in (('bg',BG,False),('fg',FG,False),('mono',MONO,True)):
        open(f'{out}/{n}.svg','w').write(to_svg(els,eo))
        if res: open(f'{res}/drawable/ic_launcher_{ {"bg":"background","fg":"foreground","mono":"monochrome"}[n] }.xml','w').write(to_vd(els,eo))
