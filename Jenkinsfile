#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2024-06'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream('knime-credentials-base/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
    ]),
    parameters(workflowTests.getConfigurationsAsParameters() + fsTests.getFSConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

SSHD_IMAGE = "${dockerTools.ECR}/knime/sshd:alpine3.11"
SMBD_IMAGE = "${dockerTools.ECR}/knime/smb:alpine3.11"

try {
    // build
    knimetools.defaultTychoBuild('org.knime.update.filehandling')

    withEnv(["KNIME_FTPD_ADDRESS=remote-docker.devops.knime.com:21"]){
    // test
        testConfigs = [
            UnitTests: {
                stage('Testing remote FS'){
                    // The integrated workflowtests only work on ubunutu at the moment
                    workflowTests.runIntegratedWorkflowTests(configurations: workflowTests.DEFAULT_FEATURE_BRANCH_CONFIGURATIONS,
                        profile: "test", sidecarContainers: [
                            [ image: SSHD_IMAGE, namePrefix: "SSHD", port: 22 ]
                    ])
            }
            },
            WorkflowTests: {
                workflowTests.runTests(
                    dependencies: [
                        repositories: [
                            'knime-gateway',
                            'knime-credentials-base',
                            'knime-cef',
                            'knime-filehandling',
                            'knime-datageneration',
                            'knime-xml',
                            'knime-js-core',
                            'knime-js-base',
                            'knime-server-client',
                            'knime-com-shared',
                            'knime-productivity-oss',
                            'knime-reporting',
                            'knime-jfreechart',
                            'knime-distance',
                            'knime-streaming',
                            'knime-kerberos',
                            'knime-ensembles'
                        ]
                    ],
                    sidecarContainers: [
                        [ image: SSHD_IMAGE, namePrefix: "SSHD", port: 22 ],
                        [ image: SMBD_IMAGE, namePrefix: "SMBD", port: 445 ]
                    ]
                )
            },
            FilehandlingTests: {
                workflowTests.runFilehandlingTests (
                    dependencies: [
                        repositories: [
                            "knime-filehandling", "knime-kerberos"
                        ]
                    ],
                )
            }
        ]

        parallel testConfigs
    }

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
