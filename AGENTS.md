## File changes and decisions

* Do not create, modify, or delete any files without prior agreement with the user.
* If any part of a task leaves room for the agent to make a choice, treat that choice as an unresolved decision and discuss it with the user before proceeding.
* Do not choose any implementation details at the agent's discretion.

## Unrequested tasks and issues

* Leave working behavior unchanged unless the requested outcome requires a change or a confirmed defect is in scope. A hypothetical failure is not a defect.
* Do not attempt to solve tasks that the user has not requested.
* Do not propose solutions or write a plan to fix an issue unless the user has requested it.
* If you notice an issue outside the user's request, you may only point it out and ask whether the user wants to address it.
* If the user declines, ignore the issue.

## Tests

* Write tests for features and their scenarios.
* For a web API, test controller endpoints by simulating HTTP requests and checking the responses.
* Do not write separate tests for intermediate actions, such as saving a resource to a database, that occur while processing a request.
* Name each test after the scenario it verifies, omitting the English grammatical articles "a", "an", and "the".
