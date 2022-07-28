FROM ghcr.io/graalvm/graalvm-ce:21.0.0
WORKDIR /opt/stags
RUN gu install native-image
RUN curl https://bintray.com/sbt/rpm/rpm > bintray-sbt-rpm.repo
RUN mv bintray-sbt-rpm.repo /etc/yum.repos.d/
RUN yum install -y sbt
ADD . ./
RUN sbt cli/graalvm-native-image:packageBin
ENTRYPOINT ["cli/target/graalvm-native-image/stags "]
