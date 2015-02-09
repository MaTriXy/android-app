#!/bin/bash -x
if [ $# -ge 2 ]
then
    if [[ "$1" = "--local" ]]
    then
        # copy CPR librrary from specified local path
        cp -a $2/. reader
     else    
        echo "Unrecognised parameter. Did you mean --local ?"
        exit 0
    fi
else 
    VERSION=1.0.14-80
    curl --user ci-mobile:changeme "http://artifactoryserver/artifactory/front-end-releases/com/blinkbox/books/web/cross-platform-reader/$VERSION/cross-platform-reader-$VERSION.zip" -o cross-platform-reader.zip
    unzip cross-platform-reader.zip reader/*
fi

echo '<!DOCTYPE html>
<html lang="en" class="no-js">
<head>
  <meta charset="utf-8" />
  <meta name="viewport"
    content="height = device-height,
    width = device-width,
    initial-scale = 1.0,
    user-scalable = no" />
	<style type="text/css">
        @font-face {
            font-family: "Roboto Slab";
            font-style: normal;
            font-weight: 400;
            src: local("Roboto Slab Regular"), local("RobotoSlab-Regular"), url(fonts/robotoslab.ttf) format("truetype");
        }

        @font-face {
            font-family: "Sorts Mill Goudy";
            font-style: normal;
            font-weight: 400;
            src: local("Sorts Mill Goudy Regular"), local("SortsMillGoudy-Regular"), url(fonts/sortsmillgoudy.ttf) format("truetype");
        }
        body{
                margin: 0; padding: 0
        }
        #reader{
            position:relative;
            top:0;
            left:0;
        }
        #reader iframe{
            position:absolute;
            top:0;
            left:0;
        }
	</style>
    <script type="text/javascript">
' > reader.html

cat reader/*jquery.min.js >> reader.html
cat reader/*reader.min.js* >> reader.html

echo '
    </script>

</head>
<body>
<!-- open content -->
  <div id="content">
      <div id="reader"></div>
  </div>
<!-- close content -->
</body>
</html>
' >> reader.html

cp reader.html ../src/main/assets/readerjs/reader.html

rm -rf dist/
rm reader.html
rm cross-platform-reader.zip
rm reader/*jquery.min.js
rm reader/*reader.min.js
rm reader/*reader.js