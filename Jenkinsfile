#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream('knime-core/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
    ]),
    parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

SSHD_IMAGE = "${dockerTools.ECR}/knime/sshd:alpine3.11"

try {

    buildConfigs = [
        Linux: {
            runIntegrationTests('workflow-tests && ubuntu18.04')
        },
        /* MacOsx: { */
        /*     runIntegrationTests('macosx') */
        /* }, */
        /* Windows: { */
        /*     runIntegrationTests('windows') */
        /* }, */
        P2Build: {
            knimetools.defaultTychoBuild('org.knime.update.filehandling')
        }
    ]

    parallel buildConfigs
    
     workflowTests.runTests(
         dependencies: [
            repositories: [
                'knime-filehandling', 'knime-datageneration', 'knime-xml',
                'knime-js-core', 'knime-js-base', 'knime-server-client', 'knime-com-shared',
                'knime-productivity-oss', 'knime-reporting', 'knime-jfreechart', 'knime-distance'
            ]
        ],
        sidecarContainers: [
            [ image: SSHD_IMAGE, namePrefix: "SSHD", port: 22 ]
        ]
     )

     stage('Sonarqube analysis') {
         env.lastStage = env.STAGE_NAME
         workflowTests.runSonar()
     }
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}

/**
* Runs integration tests, some of them require an external ssh host
*/
def runIntegrationTests(String image) {
    node(image) {
        stage('Testing remote FS'){
            env.lastStage = env.STAGE_NAME
            checkout scm

            knimetools.runIntegratedWorkflowTests(profile: "test", sidecarContainers: [
                [ image: SSHD_IMAGE, namePrefix: "SSHD", port: 22 ]
            ])
        }
    }
}

/* vim: set shiftwidth=4 expandtab smarttab: */
