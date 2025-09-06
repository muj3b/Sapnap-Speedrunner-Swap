#!/bin/bash
echo "Building SpeedrunnerSwap plugin..."
mvn clean package
echo ""
echo "If build was successful, the plugin jar can be found at:"
ls -1 target/controlswap-*.jar 2>/dev/null | tail -n 1
echo ""
