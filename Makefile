ANTLR := /Users/Andria/Desktop/Uni_Stuff/Compilers/ANTLR-4.9.3/antlr-4.9.3-complete.jar
GRAMMAR := IR.g4
MAIN_CLASS_NAME := Main
JAR_DIR := cs8803_bin
BUILD_DIR := build
COMPILER_JAR := irc.jar

ANTLR_JAVA_FILES := \
src/IRBaseListener.java \
src/IRBaseVisitor.java \
src/IRLexer.java \
src/IRListener.java \
src/IRParser.java \
src/IRVisitor.java

ANTLR_FILES := \
src/IR.interp \
src/IR.tokens \
src/IRLexer.interp \
src/IRLexer.tokens \
$(ANTLR_JAVA_FILES)

ANTLR_LIBS := \
$(BUILD_DIR)/javax \
$(BUILD_DIR)/org

SOURCES := \
src/Main.java \

.PHONY :
all: $(COMPILER_JAR)

$(COMPILER_JAR): $(SOURCES) $(ANTLR_JAVA_FILES) $(ANTLR_LIBS)
	@mkdir -p $(BUILD_DIR) $(JAR_DIR)
	@javac -d $(BUILD_DIR) -cp "src:$(ANTLR)" $(SOURCES) $(ANTLR_JAVA_FILES)
	@cd $(BUILD_DIR) && jar cfe ../$(JAR_DIR)/$(COMPILER_JAR) \
	$(MAIN_CLASS_NAME) *.class  org  javax && cd ..

$(ANTLR_JAVA_FILES): $(GRAMMAR)
	@java -jar $(ANTLR) -o src/ -visitor $(GRAMMAR)

$(ANTLR_LIBS):
	@mkdir -p  $(BUILD_DIR)
	@cd $(BUILD_DIR) &&  jar xf $(ANTLR) && cd .. @rm -rf  $(BUILD_DIR)/META-INF

.PHONY:
clean:
	@rm -f $(JAR_DIR)/$(COMPILER_JAR) $(ANTLR_FILES) $(BUILD_DIR)/*.class
	@rm -rf $(ANTLR_LIBS)

