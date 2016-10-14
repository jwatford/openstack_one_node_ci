#!/usr/bin/env groovy

def deploy_openstack_aio(release = 'master') {

    echo 'Deploying OpenStack All In One'
    git branch: release, url: 'https://github.com/openstack/openstack-ansible'
    sh """
    export apply_security_hardening=false
    sudo scripts/bootstrap-ansible.sh
    sudo scripts/bootstrap-aio.sh
    sudo scripts/run-playbooks.sh
    cd playbooks/
    sudo openstack-ansible os-tempest-install.yml
    """

}

def install_tempest() {

    echo 'Installing and configuring Tempest'
    // Install latest version of Tempest in the host
    sh '''
    git clone https://github.com/openstack/tempest.git /home/ubuntu/tempest
    cd /home/ubuntu/tempest/
    sudo pip install -r requirements.txt
    '''

    // Get a config file template with the basic static values of an OSA deployment
    sh '''
    cd /home/ubuntu/tempest/etc/
    wget https://raw.githubusercontent.com/CasJ/openstack_one_node_ci/master/tempest.conf
    '''

    // Get the tempest config file generated by the OSA deployment
    sh '''
    container_name=$(sudo lxc-ls -f | grep aio1_utility_container- | awk '{print $1}')
    sudo cp /var/lib/lxc/$container_name/rootfs/opt/tempest_untagged/etc/tempest.conf /home/ubuntu/tempest/etc/tempest.conf.osa
    '''

    // Configure the dynamic values of tempest.conf based on the OSA deployment
    sh '''
    keys='admin_password image_ref image_ref_alt uri uri_v3 public_network_id'
    for key in $keys
    do
        a="${key} ="
        b=`cat /home/ubuntu/tempest/etc/tempest.conf.osa | grep "$a"`
        sed -ir "s|$a|$b|g" /home/ubuntu/tempest/etc/tempest.conf
    done
    '''

    // Run the tests and store the results in ~/subunit/before
    sh '''
    mkdir /home/ubuntu/subunit
    cd /home/ubuntu/tempest/
    testr init
    '''

}

def run_tempest_smoke_tests(results_file = 'results') {

    sh """
    cd /home/ubuntu/tempest/
    stream_id=`cat .testrepository/next-stream`
    ostestr --no-slowest --regex smoke
    cp .testrepository/$stream_id /home/ubuntu/subunit/${results_file}
    """

}
