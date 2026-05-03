### Architecture

TestReport -> TestClass -> * Tests

Output format

```json
[{
  "className": "MyTestClass",
  "total": 20,
  "passed": 12,
  "failed": 8,
  "skipped": 0,
  "tests": [
    {
      "name": "shouldCreateOrder",
      "status": "passed"
    },
    {
      "name": "shouldRejectInvalidOrder",
      "status": "failed",
      "failure": {
        "cause": "",
        "stackTrace" : ""
      }
    }
  ]
}]
```

## Algo
Implement a TestExecutionListener

TestReport contains List<TestClass> with different hooks methods : 
* onClassStarted
  * If testIdentifier is container -> get it's id and create the TestClass and add it to the list
  * Set the startTime of the TestClass
* OnClassFinished
  * find the class in the list
  * mark this class as finished by marking the endExcutionTime
* onTestStarted
  * if testIdentifier is Test and has a Parent
  * Get the parentId which is the className (I suppose)
  * Get the TestClass based on its id
  * Get the testId of the current test and create a Test and add it to list of tests in TestClass
* onTestFinished
  * if testIdentifier is Test and has a Parent
  * Get the parentId which is the className (I suppose)
  * Get the TestClass based on its id
  * Find the test in TestClass base on the testClass id
  * update its status
  * if status = failure, get cause and stackTrace
* onTestSkipped
  *  if testIdentifier is Test and has a Parent
  * Get the parentId which is the className (I suppose)
  * Get the TestClass based on its id
  * Find the test in TestClass base on the testClass id
  * mark the status as skipped
* onTestAborted
  * if testIdentifier is Test and has a Parent
  * Get the parentId which is the className (I suppose)
  * Get the TestClass based on its id
  * Find the test in TestClass base on the testClass id
  * mark the status as skipped

At the end, buildReport() and return the TestReport object serialized in Json.

Need to take care of concurrency. 