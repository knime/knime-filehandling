#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream('knime-core/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
    ]),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

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
                'knime-productivity-oss', 'knime-reporting'
            ]
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
        def sidecars = dockerTools.createSideCarFactory()
        try {
            stage('Testing remote FS'){
                env.lastStage = env.STAGE_NAME
                checkout scm

                def ecrPrefix = "910065342149.dkr.ecr.eu-west-1.amazonaws.com/"
                def sshdImage = "knime/sshd:alpine3.10"
                def sshdhost = sidecars.createSideCar(ecrPrefix + sshdImage, 'ssh-test-host', [], [22]).start()

                def address =  sshdhost.getAddress(22)
                def testEnv = ["KNIME_SSHD_HOST=${address}"]

                knimetools.runIntegratedWorkflowTests(mvnEnv: testEnv, profile: "test")
            }
        } catch (ex) {
            currentBuild.result = 'FAILURE'
            throw ex
        } finally {
            sidecars.close()
        }
    }
}

/* vim: set shiftwidth=4 expandtab smarttab: */
