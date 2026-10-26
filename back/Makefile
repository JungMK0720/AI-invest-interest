.PHONY: test test-ml model build run
PYTHON ?= python
test: test-ml
	@mvn -B test
test-ml:
	@$(PYTHON) -m unittest discover -s ml -p 'test_*.py'
model:
	@$(PYTHON) ml/train.py
build:
	@mvn -B -DskipTests package
run:
	@mvn spring-boot:run
