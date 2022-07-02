require 'fileutils'
#Fixes the eclipse classpath. I think there are jars that are not in the right place and/or are missing
classpath_file="dev-tools/eclipse/dot.classpath"
new_classpath_file="dev-tools/eclipse/dot.classpath.fixed"
input_file=File.new(classpath_file)
output=File.new(new_classpath_file,"w")
line = input_file.gets
while !line.nil?
	#Line contains a jar entry. Check if it exists. If not, try and find, else report error and move on
	if line.include?("kind=\"lib\"")
		full_jar_file=line.scan(/path="(.+?)"/)[0][0]
		if File.exist?(full_jar_file)
			output.puts(line)
		else
			jar_file = File.basename(full_jar_file)
			puts "Going to find #{jar_file} because #{full_jar_file} doesn't exist!"
			find_results = `find . -name "#{jar_file}"`
			if find_results.empty?
			  puts "Unable to find #{jar_file} anywhere!"
			else
			  new_jar_file = find_results.strip.gsub("./","")
			  output.puts(line.gsub(full_jar_file,new_jar_file))
			end
		end
	else
		output.puts(line)
	end
	line = input_file.gets
end
output.close
FileUtils.mv(classpath_file,"#{classpath_file}.orig")
FileUtils.mv(new_classpath_file,classpath_file)
