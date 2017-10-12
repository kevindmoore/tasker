# Tasker
This library helps with sequential tasks that will be run one after another and only when each task is finished. These tasks can either be UI or background. 

## Downloading
``` 
Gradle
compile 'com.mastertechsoftware.tasker:taskerlibrary:1.0.9'
```

## Android Java Example

```
		Tasker.create().
				// Check for google Play services
				addUITask(new DefaultTask() {
					@Override
					public Object run() {
						acquireGooglePlayServices();
						return null;
					}
				})
				.withCondition(new Condition() {
					@Override
					public boolean shouldExecute() {
						return !isGooglePlayServicesAvailable();
					}
				})
				// Check to see if we have permissions
				.addUITask(new DefaultTask() {

					@Override
					public Object run() {
						String accountName = getPreferences(Context.MODE_PRIVATE)
								.getString(PREF_ACCOUNT_NAME, null);
						if (accountName != null) {
							mCredential.setSelectedAccountName(accountName);
						} else {
							startAccountChooser();
							currentDefaultTaskHandler = this;
							setPaused(true);
						}
						return null;
					}
				})
				.withCondition(new Condition() {
					@Override
					public boolean shouldExecute() {
						return EasyPermissions.hasPermissions(
								MainActivity.this, Manifest.permission.GET_ACCOUNTS);
					}
				})
				.addTask(new DefaultTask() {
					@Override
					public Object run() {
						signIn();
						setShouldContinue(false);
						return null;
					}
				})
				.addFinisher(new TaskFinisher() {
					@Override
					void finished(List<Exception> errors) {
						Logger.debug("Finished");

					}
				})
				.run();
				
	@Override
	protected void onActivityResult(
			int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
			case REQUEST_GOOGLE_PLAY_SERVICES:
				if (resultCode == RESULT_OK) {
					if (currentDefaultTaskHandler != null) {
						currentDefaultTaskHandler.setPaused(false);
						currentDefaultTaskHandler = null;
					}
				}
		}
	}				

```
##Kotlin Example
```
        Tasker.create()
                .addTask {
                		// Background Task
                    }
                .addUITask{ task ->
                	// UI Task
                }
                .run()
```
## Important Concepts
To start, just use Tasker.create(). This is in the builder format where calls can be chained together.

To run a UI Task call addUITask(Task). A DefaultTask class is provided so you can just override the run method. To add a background task call addTask. All tasks run in order so that even with background tasks, the next task won't run until that one is done.

### Conditions
If you want a task to only run when a certain condition is met, add a Condition class in withCondition. This will return true to run or false to skip.

### Stopping/Pausing
To stop processing, just call setShouldContinue(false); 

To pause a UI Task to wait for user input, call setPaused(true). When ready, call setPaused(false). In our example, I have a variable named currentDefaultTaskHandler that I set so the rest of the code knows which task to unpause.

To handle things when all tasks are done (either with a successful or error state), add a TaskFinisher to the end. (actually it can be anywhere)
### Possible Issues
The second task runs right after the first: This may happen if your first task which is run on a background thread calls another API that also runs in the background. In this case you need to tell the task to pause until the API returns. 

```
	Tasker.create().addTask { Task -> 
		task.setPaused(true)
		ApiCall.doSomeLongRunningTask( object: callback {
			override fun callbackmethod() {
				task.setPaused(false)
			}
		})
		.addUITask {
		}
		.withCondition {
			<condition check>
		}
	.run()
```

## License
Copyright 2016 Master Tech Software.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.