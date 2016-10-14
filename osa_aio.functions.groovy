#!/usr/bin/env groovy

def deploy_openstack_aio() {

    echo 'Deploying OpenStack All In One'
    git branch: 'stable/mitaka', url: 'https://github.com/openstack/openstack-ansible'
    sh """
    export apply_security_hardening=false
    scripts/bootstrap-ansible.sh
    scripts/bootstrap-aio.sh
    scripts/run-playbooks.sh
    openstack-ansible os-tempest-install.yml
    """

}

