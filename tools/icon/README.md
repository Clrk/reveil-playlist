# Icône « Macaron aube »

Un vinyle noir et brillant ; dans le macaron, le soleil se lève.

`icon.py` est la seule source : il génère les trois couches de l'icône adaptative
(`res/drawable/ic_launcher_{background,foreground,monochrome}.xml`) et un aperçu SVG.

```
python3 tools/icon/icon.py /tmp/apercu app/src/main/res
```
