#!/bin/bash
SCRIPT_DIR=`pwd`
rm -Rf buckminster/4.3
./director/2.2.0/director -r http://download.eclipse.org/tools/buckminster/headless-4.3/ -d `pwd`/buckminster/4.3 -p Buckminster -i org.eclipse.buckminster.cmdline.product
cd ./buckminster/4.3
echo "Installing core."
./buckminster install http://download.eclipse.org/tools/buckminster/headless-4.3/ org.eclipse.buckminster.core.headless.feature
echo "Patching core."
jar uf $SCRIPT_DIR/buckminster/4.3/plugins/org.eclipse.buckminster.core_*.jar -C $SCRIPT_DIR/.patch-buckminster/4.3/ .
echo "Installing git support."
./buckminster install http://download.eclipse.org/tools/buckminster/headless-4.3/ org.eclipse.buckminster.git.headless.feature
echo "Installing PDE support."
./buckminster install http://download.eclipse.org/tools/buckminster/headless-4.3/ org.eclipse.buckminster.pde.headless.feature
echo "Installing versioning support."
./buckminster install http://parking.ringler.ch/tools/buckminster/headless-4.3 org.eclipse.pde.team.feature
echo "Installing IDE support."
./buckminster install http://parking.ringler.ch/tools/buckminster/headless-4.3 org.eclipse.buckminster.ide.headless.feature
echo "Installing PDE export support."
./buckminster install http://parking.ringler.ch/tools/buckminster/headless-4.3 org.eclipse.buckminster.pde.export.headless.feature
echo "Installing GWT support."
./buckminster install http://parking.ringler.ch/tools/buckminster/headless-4.3 org.eclipse.buckminster.gwt.headless.feature
