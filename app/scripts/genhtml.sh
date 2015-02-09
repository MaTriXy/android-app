#!/bin/bash

echo '
<!DOCTYPE html>
<html>
<head>
<title>Blinkbox Books</title>

<meta name="viewport" content="width=device-width, initial-scale=1">

<link rel="stylesheet" href="http://code.jquery.com/mobile/1.2.1/jquery.mobile-1.2.1.min.css" />
<script src="http://code.jquery.com/jquery-1.8.3.min.js"></script>
<script src="http://code.jquery.com/mobile/1.2.1/jquery.mobile-1.2.1.min.js"></script>
</head>

<body>
<div data-demo-html="true"> 
<ul data-role="listview" data-inset="true">
' > index.html

for apk in $1 ; do
    #echo $apk
    ICON="`basename $apk .apk`.png"
    APP=`basename $apk`
    DESCRIPTION=`aapt dump badging $apk | grep package`
    unzip -p $apk res/drawable-xxhdpi/ic_launcher.png > $ICON

    echo "
<li><a data-ajax=\"false\" href=\"$APP\">
<img src=\"$ICON\" />
<h2>$APP</h2>
<p>$DESCRIPTION</p></a>
</li>
    " >> index.html
done

echo '</ul>
</div>
</body>
</html>
' >> index.html


