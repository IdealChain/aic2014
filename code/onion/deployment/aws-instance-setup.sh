#!/bin/bash

# execute as root

USERNAME=onion
JAVA_VERSION=1.8.0
JAVA_PACKAGES=java-$VERSION-openjdk.x86_64 java-$VERSION-openjdk-devel
MAVEN_VERSION=3.2.2
MAVEN_REMOTE=http://mirror.olnevhost.net/pub/apache/maven/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz

cd ~
echo "" > /etc/profile.d/onion.sh

# Create a new user which will be used to run the application
adduser --create-home $USERNAME
mkdir /home/$USERNAME/.ssh
cp /home/ec2-user/.ssh/authorized_keys /home/$USERNAME/.ssh/authorized_keys
chmod -R 700 /home/$USERNAME/.ssh/
chown -R onion:onion /home/$USERNAME/.ssh/

# Install Java 
yum --assumeyes install JAVA_PACKAGES
yum --assumeyes remove java-1.7.0-openjdk.x86_64
echo "export JAVA_HOME=/usr/lib/jvm/jre" >> /etc/profile.d/onion.sh

# Install Maven
mkdir /opt/apache-maven
cd /opt/apache-maven
wget MAVEN_REMOTE
tar xvf apache-maven-$MAVEN_VERSION-bin.tar.gz
rm apache-maven-$MAVEN_VERSION-bin.tar.gz
chmod -R +rx *
cd ~
echo 'export M2_HOME=/opt/apache-maven/apache-maven-$MAVEN_VERSION' >> /etc/profile.d/onion.sh
echo 'export M2=$M2_HOME/bin' >> /etc/profile.d/onion.sh
echo 'export PATH=$M2:$PATH' >> /etc/profile.d/onion.sh

# Prepare onion home dir
runuser -l onion --command "mkdir chainnode"