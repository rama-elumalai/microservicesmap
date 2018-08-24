FROM 460570964411.dkr.ecr.us-west-2.amazonaws.com/charter/amznlinux:latest

LABEL maintainer "monson.xavier@charter.com"

# Install Common Dependencies
RUN yum update -y \
  && yum install charter-jdk8-0.0.5-32 -y \
  && yum install charter-cacerts-jdk8 -y \
  && yum install jq -y \
  && yum install wget -y \
  && yum install python -y \
  && yum install python3 -y \
  && yum install curl -y \
  && curl "https://bootstrap.pypa.io/get-pip.py" -o "get-pip.py" \
  && python get-pip.py \
  && pip install pexpect -y \
  && yum install groovy -y \
  && wget http://apache.claz.org/tinkerpop/3.3.3/apache-tinkerpop-gremlin-server-3.3.3-bin.zip \
  && unzip -p apache-tinkerpop-gremlin-server-3.3.3-bin.zip -d /opt/charter \
