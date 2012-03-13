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
	find . -name '*.png' -print >> $(CLASS_LIST); \
	find misc/ -name '*.html' -print >> $(CLASS_LIST);

$(JAR_FILE): 
	@if ! ls *.class ; then \
		echo "No .class files found - please run make compile."; \
		rm $(CLASS_LIST); \
		exit 1; \
	fi
	jar -cv0mf manifest.txt $@ @$(CLASS_LIST) -J-Xmx500M; \
	rm $(CLASS_LIST)

$(ZIP_FILE): $(JAR_FILE)
	dir="VarSifter_$(VERSION)"; \
	mkdir $${dir}; \
	exec="java -Xmx500M -jar VarSifter_$(VERSION).jar"; \
	echo -e "#!/bin/bash\ncd \`dirname \$$0\`\n$${exec}" > $${dir}/VarSifter_$(VERSION).command && chmod 755 $${dir}/VarSifter_$(VERSION).command; \
	echo "$${exec}" > $${dir}/VarSifter_$(VERSION).bat && chmod 755 $${dir}/VarSifter_$(VERSION).bat; \
	cp jung/jung-graph-impl-2.0.1.jar $$dir; \
	cp README.txt $$dir; \
	cp JUNG_bsd_license.txt $$dir; \
	cp apache2_license.txt $$dir; \
	cp jung/jung-visualization-2.0.1.jar $$dir; \
	cp jung/jung-algorithms-2.0.1.jar $$dir; \
	cp jung/jung-api-2.0.1.jar $$dir; \
	cp jung/collections-generic-4.01.jar $$dir; \
	cp jung/json-simple-1.1.1.jar $$dir; \
	cp *.json $$dir; \
	cp $< $$dir; \
	zip -r $@ $$dir; \
	rm -rf $$dir/

$(SRC_FILE): clean
	tar -cvzf $@ *.java images/* misc/* components/*.java Makefile manifest.txt BUILD.txt;


docs:
	-@mkdir html; \
	cd html; \
	javadoc -J-Xmx500M -sourcepath ../ ../*.java

docs_private:
	-@mkdir html; \
	cd html; \
	javadoc -J-Xmx500M -private -sourcepath ../ ../*.java
	

clean:
	-rm *class */*class *jar *zip *.src.tgz VarSifter_*.command VarSifter_*.bat classes.txt; \
	rm -rf html/*
