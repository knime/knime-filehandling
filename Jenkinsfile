#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream('knime-base/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
    ]),
    parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

SSHD_IMAGE = "${dockerTools.ECR}/knime/sshd:alpine3.11"

try {

    buildConfigs = [
        UnitTests: {
            stage('Testing remote FS'){
                // The integrated workflowtests only work on ubunutu at the moment
                workflowTests.runIntegratedWorkflowTests(configurations: ['ubuntu18.04'],
                    profile: "test", sidecarContainers: [
                        [ image: SSHD_IMAGE, namePrefix: "SSHD", port: 22 ]
                ])
           }
        },
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
                'knime-productivity-oss', 'knime-reporting', 'knime-jfreechart', 'knime-distance',
                'knime-streaming'
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

/* vim: set shiftwidth=4 expandtab smarttab: */
