#Change these to point to the appropriate java compiler binaries
JAVAC5=/home/teerj/bin/javac
JAVAC6=javac

#Define this when calling make, ie make VERSION=1.0
VERSION:=9999

CLASS_LIST:=classes.txt
JAR_FILE:=VarSifter_$(VERSION).jar
ZIP_FILE:=VarSifter_$(VERSION).zip
SRC_FILE:=VarSifter_$(VERSION).src.tgz

all: compile6

compile5: clean
	${JAVAC5} VarSifter.java

compile_check: clean
	${JAVAC6} -J-Xmx1G -cp jung/*:. -Xlint:unchecked VarSifter.java

compile6: clean
	${JAVAC6} -J-Xmx1G -cp jung/*:. -target 5 VarSifter.java

build: $(CLASS_LIST) $(JAR_FILE)
zip: $(CLASS_LIST) $(JAR_FILE) $(ZIP_FILE)
src: $(SRC_FILE)

$(CLASS_LIST):
	@find . -name '*.class' -print > $(CLASS_LIST); \
	find . -name '*.png' -print >> $(CLASS_LIST);

$(JAR_FILE): 
	@if [ ! -s "$(CLASS_LIST)" ]; then \
		echo "No .class files found - please run make compile."; \
		rm $(CLASS_LIST); \
		exit 1; \
	fi
	jar -cv0mf manifest.txt $@ @$(CLASS_LIST) -J-Xmx500M; \
	rm $(CLASS_LIST)

$(ZIP_FILE): $(JAR_FILE)
	exec="java -Xmx500M -jar VarSifter_$(VERSION).jar"; \
	echo -e "#!/bin/bash\ncd \`dirname \$$0\`\n$${exec}" > VarSifter_$(VERSION).command && chmod 755 VarSifter_$(VERSION).command; \
	echo "$${exec}" > VarSifter_$(VERSION).bat && chmod 755 VarSifter_$(VERSION).bat; \
	zip $@ -j $< jung/jung-graph-impl-2.0.1.jar README.txt JUNG_bsd_license.txt apache2_license.txt \
	jung/jung-visualization-2.0.1.jar jung/jung-algorithms-2.0.1.jar jung/jung-api-2.0.1.jar \
	jung/collections-generic-4.01.jar VarSifter_$(VERSION).sh VarSifter_$(VERSION).bat;

$(SRC_FILE): clean
	tar -cvzf $@ *.java images/* components/*.java Makefile manifest.txt BUILD.txt;


docs:
	-@mkdir html; \
	cd html; \
	javadoc -J-Xmx500M -sourcepath ../ ../*.java

docs_private:
	-@mkdir html; \
	cd html; \
	javadoc -J-Xmx500M -private -sourcepath ../ ../*.java
	

clean:
	-rm *class */*class *jar *zip *.src.tgz VarSifter_*.sh VarSifter_*.bat classes.txt; \
	rm -rf html/*
