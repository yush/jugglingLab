#!/bin/csh -f

cd ..
cd Juggling\ Lab.app/Contents/Resources/Java
java -cp Juggling\ Lab.jar jugglinglab/generator/siteswapGenerator $*
