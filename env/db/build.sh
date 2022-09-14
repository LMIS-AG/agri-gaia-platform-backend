docker rm agrigaia-platform-db -f
docker image rm agrigaia-platform-db -f
docker build -t agrigaia-platform-db .
docker run -d -p 3306:3306 --name agrigaia-platform-db agrigaia-platform-db