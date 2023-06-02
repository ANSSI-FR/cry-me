#!/bin/bash

function get_sdk_image {
    URL=$1
    DEST_PATH=$2
    curl $1 -o $2
}

get_sdk_image https://www.cryptoexperts.com/cry-me/matrix-sdk.tar.gz $1
