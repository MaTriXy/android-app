#!/bin/sh
function grabbook() {
    ISBN=$1
    DIR=$2
    JSON="curl -s \"catalogue service/books/$ISBN\" | python -m json.tool > json.txt"; eval $JSON
    COVER_URL=$(cat json.txt | grep src | cut -d \" -f 4)
    EPUB_URL=$(cat json.txt | grep href | grep epub | cut -d \" -f 4)

    EPUB_FILE="$ISBN.epub"
    COVER_FILE="$ISBN""_cover_image.png"

    splitCOVER=(${COVER_URL//.com/ })
    COVER_URL="${splitCOVER[0]}.com/params;img:w=900;v=0${splitCOVER[1]}"

    curl "$EPUB_URL" -o $EPUB_FILE
    curl "$COVER_URL" -o $COVER_FILE

    zip $EPUB_FILE $COVER_FILE

    mkdir -p $DIR
    cp $EPUB_FILE $DIR
    rm $EPUB_FILE
    rm $COVER_FILE
    rm json.txt
}

# books for version 1.1
#grabbook 9781405514675 ../src/main/assets/books/
#grabbook 9781447248743 ../src/main/assets/books/

#books for version 1.0
grabbook 9780230760653 ../src/main/assets/books/
grabbook 9781444761191 ../src/main/assets/books/
grabbook 9781444765588 ../src/main/assets/books/