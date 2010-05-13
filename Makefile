JAVAC5=/home/teerj/bin/javac
JAVAC6=javac

CLASS_LIST:=classes.txt
JAR_FILE:=VarSifter.jar

all: compile6

compile5:
	${JAVAC5} VarSifter.java

compile_check:
	${JAVAC5} -Xlint:unchecked VarSifter.java

compile6:
	${JAVAC6} -target 5 VarSifter.java

build: $(CLASS_LIST) $(JAR_FILE)

$(CLASS_LIST):
	@find . -name '*.class' -print > $(CLASS_LIST)

$(JAR_FILE): 
	@if [ ! -s "$(CLASS_LIST)" ]; then \
		echo "No .class files found - please run make compile."; \
		rm $(CLASS_LIST); \
		exit 1; \
	fi
	jar -cv0mf manifest.txt $@ @$(CLASS_LIST); \
	rm $(CLASS_LIST)
	

clean:
	-rm *class */*class *jar
