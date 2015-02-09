#!/bin/sh

for i in * ; do
    if [ -d "$i" ] && [ ! -e "$i.epub" ]; then
        echo "Building $i.epub..."
        cd $i
        zip ../$i.epub mimetype
        zip ../$i.epub META-INF/
        zip ../$i.epub META-INF/container.xml
        zip ../$i.epub -r *
        cd -
    fi
done

