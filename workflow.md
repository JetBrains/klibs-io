# Workflow – Klibs backend Team
This document describes the branching strategy and release flow for our backend projects.

## Branching Strategy

* **master**

  * Always reflects the production state.

  * Deployed automatically to production.


* **release***

  * Current release branch.

  * Stores only verified tasks.

  *  Deployed to the test environment before release for E2E testing.

  *  Merged into master during release.


* **feature/KTL-54321-***

  * Feature branches, created from the current `release` branch.

  * Tested on the `test` environment before merging.

  * Naming: `feature/KTL-<task-id>-<short-description>`


* **hotfix/KTL-54321-***

  * Hotfix branches are created directly from master.

  * Naming: `hotfix/KTL-<task-id>-<short-description>`

## Feature development flow
This block describees working on a new feature flow.

1. Create a branch from the current `release`:
    ```bash
    git checkout -b feature/KTL-<task-id>-<short-description> release
    ```
2. Implement your changes in this branch.
3. Move task to `Code Review` state in YouTrack.
4. Open a Merge Request to a `release` branch. 
5. After review approval: [Deploy the feature](#how-to-deploy-a-feature-to-the-features-environment) branch to the `features` environment and move task to `Ready for Testing` state in YouTrack.
6. When start testing, move a task to `Testing` state in YouTrack.
7. After successful testing, merge the feature branch into the `release`.
8. Move task to `Ready for Deploy` state in YouTrack.

## Project tag updates
When updating project tags via `/content/project/tags`, the allowed tags list comes from
`integrations/ai/src/main/resources/ai/prompts/tag_rules.yaml`.

1. If a new tag is required, add a new entry to `tag_rules` with the canonical `name` and its metadata.
2. If the tag is just a synonym of an existing one, add it to that tag's `tag_synonyms` list instead.

## Hotfix Flow
1. Create a branch from `master`:
    ```bash
      git checkout -b hotfix/KTL-<task-id>-<short-description> master
    ```
2. Implement your fix in this branch.
3. Move task to `Code Review` state in YouTrack.
4. Open a Merge request to the `master` branch.
5. After review approval: move the task to `Ready for Testing` state in YouTrack.
6. When start testing, move the task to `Testing` state in YouTrack.
7. [Deploy the hotfix](#how-to-deploy-a-feature-to-the-features-environment) branch to the `features` environment. 
8. Move task to `Ready for Deploy` state in YouTrack.
9. If tests pass, merge the hotfix branch into `master`. 
10. Wait for production deployment.
11. Monitor production logs for suspicious exceptions. 
12. Verify production with manual smoke testing.
13. Rebase `release` branch onto `master`.
14. Force push `release` branch.
15. When the hotfix is in `master` and `release` branch move task to `Fixed` state in YouTrack.


## Release Flow
This block describes working on a release flow.
1. Deploy the current `release` to the `test` environment.
2. Verify successful deployment.
3. Check indexing performance:
   1. Enable indexing in [ConfigMap](https://console.intellij.net/config-map?clusterId=gke-europe-west1&namespace=klibs-test&config-map-name=klibs-features-config)
   2. Wait for the nightly run and check for errors and exceptions
4. (TODO: KTL-2947) Run automated E2E / integration tests against the deployed branch.
5. Perform manual smoke testing for critical scenarios:
    * Search project
    * Search package
    * Open the project page
    * Open package page
6. When tests pass, merge `release` into `master` via merge request.
7. Wait for production deployment from `master`.
8. Monitor production logs for suspicious exceptions.
9. Perform manual smoke tests in production.
10. Create and push tag for the current release format: `release-yyyy.mm.dd`.
11. Announce deployment completion in Slack: [#ked-websites-private](https://jetbrains.slack.com/archives/G017S6Z1P5Y) channel

## How to deploy a feature to the features environment
#### Prerequisites:
* you have kubectl with prod environment setup

#### Steps:
1. Before deploying your branch to the test environment, you should apply a recent dump from prod.
   * in the terminal run:
   ```bash
   cd scripts/kubernetes && kubectl -n klibs-features apply -f apply-prod-dump-on-klibs-features-environment-job.yaml
   ```
2. Wait until the job is completed ([logs are here](https://grafana.intellij.net/goto/cVJGtu6Hg?orgId=1))
    * You can use this command to check the job status too:
    ```bash
    kubectl -n klibs-features get job pg-copy-dump-to-klibs-features-environment-manual -o wide
    ```
3. Ensure that settings in the [ConfigMap](https://console.intellij.net/config-map?clusterId=gke-europe-west1&namespace=klibs-features&config-map-name=klibs-features-config) are correct for your branch. 
4. Go to the `features` cloud console environment [deployment settings](https://console.intellij.net/cicd-app-jib-edit?clusterId=gke-europe-west1&namespace=klibs-features&name=klibs-features-backend) and change the _**Default branch**_ field to your branch.
5. Save the settings and wait until [build-klibs-features-backend](https://teamcity-it.intellij.net/buildConfiguration/KubernetesController_GkeBelgiumEuropeWest1_klibs_features_teamcity_it_generated_project_BuildKlibsFeaturesBackend#all-projects) and then [deploy-klibs-features-backend](https://teamcity-it.intellij.net/buildConfiguration/KubernetesController_GkeBelgiumEuropeWest1_klibs_features_teamcity_it_generated_project_DeployKlibsFeaturesBackend#all-projects) pass with your changes