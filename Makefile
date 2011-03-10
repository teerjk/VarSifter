JAVAC5=/home/teerj/bin/javac
JAVAC6=javac

CLASS_LIST:=classes.txt
JAR_FILE:=VarSifter.jar

all: compile6

compile5:
	${JAVAC5} VarSifter.java

compile_check:
	${JAVAC6} -J-Xmx1G -cp jung/*:. -Xlint:unchecked VarSifter.java

compile6: clean
	${JAVAC6} -J-Xmx1G -cp jung/*:. -target 5 VarSifter.java

build: $(CLASS_LIST) $(JAR_FILE)

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

docs:
	@cd html; \
	javadoc -J-Xmx500M -private -sourcepath ../ ../*.java
	

clean:
	-rm *class */*class *jar; \
	rm -rf html/*
