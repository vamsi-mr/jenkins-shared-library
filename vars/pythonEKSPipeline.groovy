def call(Map configMap){
    pipeline {
        agent {
            node {
            label 'AGENT-1'
            }
        }
        environment {
            appVersion = ''
            REGION = 'us-east-1'
            ACC_ID = '025523569021'
            PROJECT = configMap.get('project')
            COMPONENT = configMap.get('component')
        }
        options {
            timeout(time: 30, unit: 'MINUTES')
            disableConcurrentBuilds()
        }

        parameters {
            booleanParam(name: 'deploy', defaultValue: false, description: 'Toggle this value')
        }       

        stages{ 
            stage('Read version') {
                steps {
                    script {
                        appVersion = readFile('version').trim()
                        echo "Package version : ${appVersion}"
                    }
                }
            }
            stage('Installing Dependencies') {
                steps {
                    script {
                        sh """
                            pip3 install -r requirements.txt
                        """
                    }
                }
            }
            stage('Unit Testing') {
                steps {
                    script {
                        sh """
                            echo "Unit Testing"
                        """
                    }
                }
            }
            stage('Docker Build') {
                steps {
                    script {
                        withAWS(credentials: 'aws-creds', region: '${REGION}') {
                            sh """
                                aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com
                                docker build -t ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .
                                docker push ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                                aws ecr wait image-scan-complete --repository-name ${PROJECT}/${COMPONENT} --image-id imageTag=${appVersion} --region ${REGION}
                            """
                        }
                    }
                }
            }
            stage('Trigger Deploy') {
                when{
                    expression { params.deploy }
                }
                steps {
                    script {
                        //build job: 'catalogue-cd',
                        build job: "../${COMPONENT}-cd",
                        parameters: [
                            string(name: 'appVersion', value: "${appVersion}"),
                            string(name: 'deploy_to', value: 'dev')
                        ],
                        propagate: false,  // even SG fails VPC will not be effected
                        wait: false // VPC will not wait for SG pipeline completion
                    }
                }
            }
        }
        
        post {
            always {
                echo 'I will always says Hello again!'
                script {
                    deleteDir()
                }
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