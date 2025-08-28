def call(Map configMap){
    pipeline {
        agent {
            node {
            label 'AGENT-1'
            }
        }
        environment {
            COURSE = 'Jenkins'
            greeting = configMap.get('greeting')
        }
        options {
            timeout(time: 30, unit: 'MINUTES')
            disableConcurrentBuilds()
        }
        parameters {
            string(name: 'PERSON', defaultValue: 'Mr Mohan Vamsi Ravada', description: 'How are you')
        }

        stages{ 
            stage('Build') {
                steps {
                    script {
                        sh """
                            echo "Hello Build..."
                            sleep 10
                            env
                            echo "Hello ${params.PERSON}"
                        """
                    }
                }
            }
            stage('Test') {
                steps {
                    script {
                        sh """
                            echo "Hello Testing..."
                            env
                        """
                    }
                }
            }
        }

        post {
            always {
                echo 'I will always says Hello again!'
                deleteDir()
            }
            success {
                echo 'Hello Success'
            }
            failure {
                echo 'Hello Failure'
            }
        }
    }
}

