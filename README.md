# Tasker
This library helps with sequential tasks that will be run one after another and only when each task is finished. These tasks can either be UI or background. 

## Downloading
``` 
Gradle
compile 'com.mastertechsoftware.tasker:taskerlibrary:1.0.4'
```

## Usage Example

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
						shouldContinue = false;
						return null;
					}
				})
				.addFinisher(new TaskFinisher() {
					@Override
					public void onSuccess() {
						Logger.debug("Finished successfully");

					}

					@Override
					public void onError() {
						Logger.debug("Finished with errors");

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
