#!/usr/bin/env ruby

require "rubygems"
require "json"
require "net/http"
require "uri"

# Reset DB
reset_db_uri = URI.parse("http://localhost:8888/admin/com.magnet.server/db/objects")

Net::HTTP.new(reset_db_uri.host, reset_db_uri.port).start do |mms_client|
  reset_db_request  = Net::HTTP::Put.new(reset_db_uri.path)
  reset_db_response = mms_client.request(reset_db_request)
  if reset_db_response.code == "200"
    authenticate_admin_uri = URI.parse("http://localhost:8888/admin/com.magnet.server/users/session")
    Net::HTTP.new(authenticate_admin_uri.host, authenticate_admin_uri.port).start do |mms_admin_client|
      authenticate_admin_request                 = Net::HTTP::Post.new(authenticate_admin_uri.path)
      authenticate_admin_request.body            = "username=admin&password=admin"
      authenticate_admin_request["Content-Type"] = "application/x-www-form-urlencoded"

      authenticate_admin_response = mms_admin_client.request(authenticate_admin_request)

      if authenticate_admin_response.code == "200"
        authenticate_admin_result = JSON.parse(authenticate_admin_response.body)
        access_token = authenticate_admin_result["access_token"]

        create_app_uri = URI.parse("http://localhost:8888/admin/com.magnet.server/apps/enrollment")
        create_app_request                  = Net::HTTP::Post.new(create_app_uri.path)
        create_app_request.body             = "{\"tag\":\"mobile\",\"client_name\":\"test.magnet.com\",\"client_description\":\"Test Application\",\"owner_email\":\"no-reply@magnet.com\",\"password\":\"password\"}"
        create_app_request["Authorization"] = "Bearer #{access_token}"
        create_app_request["Content-Type"]  = "application/json"

        create_app_response = mms_admin_client.request(create_app_request)

        if create_app_response.code == "200"
          create_app_result = JSON.parse(create_app_response.body)
          puts "writing data to keys.json ...\n\n"
          puts create_app_response.body
          File.open('src/androidTest/res/raw/keys.json', 'w') { |file| file.write(create_app_response.body) }
        end

      else
        puts "ERROR authenticating admin!"
      end
    end
  else
    puts "ERROR resetting database!"
  end
end
