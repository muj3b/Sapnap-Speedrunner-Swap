#!/bin/bash
echo "Packaging SpeedrunnerSwap source code..."
zip -r SpeedrunnerSwap_v2.3.0_full_source.zip . -x "*.git*" "target/*" "*.zip"
echo "Source code packaged to SpeedrunnerSwap_v2.3.0_full_source.zip"