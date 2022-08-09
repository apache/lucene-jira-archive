#include <iostream>
#include <string>
#include <cstdio>
#include <algorithm>
#include <cassert>
#include <map>
#include <cfloat>

using namespace std;


#define EPS 0.1
#define BIG_THOROLD 0.3


inline double maxItemValue(const map<string, double> &items) {
	double max = DBL_MIN;
	for (auto it = items.begin(); it != items.end(); it ++) {
		if ( it->second > max ) max = it->second;
	}
	return max;
}


ostream& operator << (ostream &out, 
		const map<string, map<string, double>> tasks) {
	char line[100];
	sprintf(line, "%20s", "Task");
	cout << line;

	const auto &first = tasks.begin()->second;
	for (auto it = first.begin(); it != first.end(); it ++) {
		sprintf(line, " %12s", it->first.c_str());
		cout << line;
	}
	cout << endl;

	string pattern = "%12.1f ";
	for (auto it = tasks.begin(); it != tasks.end(); it ++) {
		sprintf(line, "%20s", it->first.c_str());
		cout << line;

		const double maxValue = maxItemValue(it->second);
		for (auto jt = it->second.begin(); jt != it->second.end(); jt ++) {
			pattern.back() = ' ';
			const double DELTA = maxValue - jt->second;
			if ( jt->second > 0 ) {
				if ( DELTA < EPS ) pattern.back() = '*';
				else if ( DELTA / maxValue < BIG_THOROLD ) pattern.back() = '+';
			}
			sprintf(line, pattern.c_str(), jt->second);
			cout << line;
		}
		cout << endl;
	}
	cout << endl;

	sprintf(line, "%20s%20s", "Task", "Good Method");
	cout << line << endl;
	for (auto it = tasks.begin(); it != tasks.end(); it ++) {
		sprintf(line, "%20s", it->first.c_str());
		cout << line;
		vector<pair<double, string>> reverseItems;
		for (auto jt = it->second.begin(); jt != it->second.end(); jt ++) {
			reverseItems.push_back(make_pair(jt->second, jt->first));
		}
		sort(reverseItems.begin(), reverseItems.end());

		cout << "       ";
		const double maxValue = reverseItems.rbegin()->first;
		for (auto jt = reverseItems.rbegin(); jt != reverseItems.rend(); jt ++) {
			const double DELTA = maxValue - jt->first;
			if ( jt->first > 0 && DELTA / maxValue < BIG_THOROLD ) {
				cout << jt->second << ", ";
			}
		}
		cout << endl;
	}
	cout << endl;


	return out;
}


int main() {
	string test;
	map<string, map<string, double>> tasks;
	if ( !(cin >> test) ) return -1;

	while ( true ) {
		if ( test.substr(0, 2) != "--" ) break;

		test = test.substr(2);
		string ignore;
		getline(cin, ignore);
		getline(cin, ignore);
		assert(ignore.find("TaskQPS") != string::npos);

		string task;
		while (cin >> task) {
			if ( task.substr(0, 2) == "--" ) {
				test = task;
				break;
			}
			if ( task == "PKLookup" ) {
				getline(cin, ignore);
				continue;
			}

			cin >> ignore >> ignore >> ignore >> ignore;
			double value;
			cin >> value;
			getline(cin, ignore);

			tasks[task][test] = value;
		}
	}

	cout << tasks << endl;

	return 0;
}


